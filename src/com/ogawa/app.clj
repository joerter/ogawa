(ns com.ogawa.app
  (:require
   [cheshire.core :as cheshire]
   [com.biffweb :as biff :refer [q]]
   [com.ogawa.middleware :as mid]
   [com.ogawa.ui :as ui]
   [ring.adapter.jetty9 :as jetty]
   [rum.core :as rum]
   [xtdb.api :as xt]))

(defn app [ctx]
  (ui/app-page
   ctx
   (biff/form
    {:action "/stream"}
    [:button.btn.w-full {:type "submit"} "New Stream"])))

(defn new-community [{:keys [session] :as ctx}]
  (let [comm-id (random-uuid)]
    (biff/submit-tx ctx
                    [{:db/doc-type :community
                      :xt/id comm-id
                      :comm/title (str "Community #" (rand-int 1000))}
                     {:db/doc-type :membership
                      :mem/user (:uid session)
                      :mem/comm comm-id
                      :mem/roles #{:admin}}])
    {:status 303
     :headers {"Location" (str "/community/" comm-id)}}))

(defn new-stream [{:keys [session] :as ctx}]
  (let [stream-id (random-uuid)]
    (biff/submit-tx ctx
                    [{:db/doc-type :stream
                      :xt/id stream-id
                      :stream/title (str "Stream #" (rand-int 1000))}
                     {:db/doc-type :membership
                      :mem/user (:uid session)
                      :mem/stream stream-id
                      :mem/roles #{:admin}}])
    {:status 303
     :headers {"Location" (str "/stream/" stream-id)}}))

(defn join-community [{:keys [user community] :as ctx}]
  (biff/submit-tx ctx
                  [{:db/doc-type :membership
                    :db.op/upsert {:mem/user (:xt/id user)
                                   :mem/comm (:xt/id community)}
                    :mem/roles [:db/default #{}]}])
  {:status 303
   :headers {"Location" (str "/community/" (:xt/id community))}})

(defn new-channel [{:keys [community roles] :as ctx}]
  (if (and community (contains? roles :admin))
    (let [chan-id (random-uuid)]
      (biff/submit-tx ctx
                      [{:db/doc-type :channel
                        :xt/id chan-id
                        :chan/title (str "Channel #" (rand-int 1000))
                        :chan/comm (:xt/id community)}])
      {:status 303
       :headers {"Location" (str "/community/" (:xt/id community) "/channel/" chan-id)}})
    {:status 403
     :body "Forbidden."}))

(defn delete-channel [{:keys [biff/db channel roles] :as ctx}]
  (when (contains? roles :admin)
    (biff/submit-tx ctx
                    (for [id (conj (q db
                                      '{:find msg
                                        :in [channel]
                                        :where [[msg :msg/channel channel]]}
                                      (:xt/id channel))
                                   (:xt/id channel))]
                      {:db/op :delete
                       :xt/id id})))
  [:<>])

(defn community [{:keys [biff/db user community] :as ctx}]
  (let [member (some (fn [mem]
                       (= (:xt/id community) (get-in mem [:mem/comm :xt/id])))
                     (:user/mems user))]
    (ui/app-page
     ctx
     (if member
       [:<>
        [:.border.border-neutral-600.p-3.bg-white.grow
         "Messages window"]
        [:.h-3]
        [:.border.border-neutral-600.p-3.h-28.bg-white
         "Compose window"]]
       [:<>
        [:.grow]
        [:h1.text-3xl.text-center (:comm/title community)]
        [:.h-6]
        (biff/form
         {:action (str "/community/" (:xt/id community) "/join")
          :class "flex justify-center"}
         [:button.btn {:type "submit"} "Join this community"])
        [:div {:class "grow-[1.75]"}]]))))

(defn stream [{:keys [biff/db user stream roles] :as ctx}]
  (let [href (str "/stream/" (:xt/id stream))]
    (ui/app-page
     ctx
     [:div [:h1 "Welcome to the show"]
      [:div.w-full.flex.justify-center
       [:video.w-full
        {:id "streamVideo" :autoplay true :playsinline true :controls false}]]
      (if (contains? roles :admin)
        [:div
         [:button.btn {:type "button" :id "startStreamButton"} "Start the Stream"]
         [:script {:src "/streamer.js" :type "module"}]]
        [:div {:hx-ext "ws" :ws-connect (str href "/connect")}
         (biff/form {:hx-post (str href "/join")
                     :hx-target "#joinStreamButton"}
                    [:input {:type "hidden" :id "joinOffer" :name "join-offer" :value ""}]
                    [:button.btn {:type "submit" :id "joinStreamButton" :disabled true} "Join the Stream"])
         [:script {:src "/viewer.js" :type "module"}]])])))

(defn join-stream [{:keys [biff/db user stream params] :as ctx}]
  (let [join-offer (-> params :join-offer cheshire/parse-string)
        offer {:db/doc-type :offer
               :xt/id (random-uuid)
               :offer/sdp (get join-offer "sdp")
               :offer/type (get join-offer "type")
               :offer/stream (:xt/id stream)
               :offer/user (:xt/id user)}]
    (biff/submit-tx ctx [offer])
    [:div "you joined the stream"]))

(defn message-view [{:msg/keys [mem text created-at]}]
  (let [username (str "User " (subs (str mem) 0 4))]
    [:div
     [:.text-sm
      [:span.font-bold username]
      [:span.w-2.inline-block]
      [:span.text-gray-600 (biff/format-date created-at "d MMM h:mm aa")]]
     [:p.whitespace-pre-wrap.mb-6 text]]))

(defn new-message [{:keys [channel mem params] :as ctx}]
  (let [msg {:xt/id (random-uuid)
             :msg/mem (:xt/id mem)
             :msg/channel (:xt/id channel)
             :msg/created-at (java.util.Date.)
             :msg/text (:text params)}]
    (biff/submit-tx (assoc ctx :biff.xtdb/retry false)
                    [(assoc msg :db/doc-type :message)])
    [:<>]))

(defn channel-page [{:keys [biff/db community channel] :as ctx}]
  (let [msgs (q db
                '{:find (pull msg [*])
                  :in [channel]
                  :where [[msg :msg/channel channel]]}
                (:xt/id channel))
        href (str "/community/" (:xt/id community)
                  "/channel/" (:xt/id channel))]
    (ui/app-page
     ctx
     [:.border.border-neutral-600.p-3.bg-white.grow.flex-1.overflow-y-auto#messages
      {:hx-ext "ws"
       :ws-connect (str href "/connect")
       :_ "on load or newMessage set my scrollTop to my scrollHeight"}
      (map message-view (sort-by :msg/created-at msgs))]
     [:.h-3]
     (biff/form
      {:hx-post href
       :hx-target "#messages"
       :hx-swap "beforeend"
       :_ (str "on htmx:afterRequest"
               " set <textarea/>'s value to ''"
               " then send newMessage to #messages")
       :class "flex"}
      [:textarea.w-full#text {:name "text"}]
      [:.w-2]
      [:button.btn {:type "submit"} "Send"]))))

(defn connect-stream [{:keys [com.ogawa/stream-clients] {stream-id :xt/id} :stream :as ctx}]
  {:status 101
   :headers {"upgrade" "websocket"
             "connection" "upgrade"}
   :ws {:on-connect (fn [ws]
                      (prn :connect (swap! stream-clients update stream-id (fnil conj #{}) ws)))
        :on-close (fn [ws status-code reason]
                    (prn :disconnect
                         (swap! stream-clients
                                (fn [stream-clients]
                                  (let [stream-clients (update stream-clients stream-id disj ws)]
                                    (cond-> stream-clients
                                      (empty? (get stream-clients stream-id)) (dissoc stream-id)))))))}})

(defn connect [{:keys [com.ogawa/chat-clients] {chan-id :xt/id} :channel :as ctx}]
  {:status 101
   :headers {"upgrade" "websocket"
             "connection" "upgrade"}
   :ws {:on-connect (fn [ws]
                      (prn :connect (swap! chat-clients update chan-id (fnil conj #{}) ws)))
        :on-close (fn [ws status-code reason]
                    (prn :disconnect
                         (swap! chat-clients
                                (fn [chat-clients]
                                  (let [chat-clients (update chat-clients chan-id disj ws)]
                                    (cond-> chat-clients
                                      (empty? (get chat-clients chan-id)) (dissoc chan-id)))))))}})

(defn on-new-message [{:keys [biff.xtdb/node com.ogawa/chat-clients]} tx]
  (let [db-before (xt/db node {::xt/tx-id (dec (::xt/tx-id tx))})]
    (doseq [[op & args] (::xt/tx-ops tx)
            :when (= op ::xt/put)
            :let [[doc] args]
            :when (and (contains? doc :msg/text)
                       (nil? (xt/entity db-before (:xt/id doc))))
            :let [html (rum/render-static-markup
                        [:div#messages {:hx-swap-oob "beforeend"}
                         (message-view doc)
                         [:div {:_ "init send newMessage to #messages then remove me"}]])]
            ws (get @chat-clients (:msg/channel doc))]
      (jetty/send! ws html))))

(defn wrap-community [handler]
  (fn [{:keys [biff/db user path-params] :as ctx}]
    (if-some [community (xt/entity db (parse-uuid (:id path-params)))]
      (let [mem (->> (:user/mems user)
                     (filter (fn [mem]
                               (= (:xt/id community) (get-in mem [:mem/comm :xt/id]))))
                     first)
            roles (:mem/roles mem)]
        (handler (assoc ctx :community community :roles roles :mem mem)))
      {:status 303
       :headers {"location" "/app"}})))

(defn wrap-stream [handler]
  (fn [{:keys [biff/db user path-params] :as ctx}]
    (if-some [stream (xt/entity db (parse-uuid (:id path-params)))]
      (let [mem (->> (:user/mems user)
                     (filter (fn [mem]
                               (= (:xt/id stream) (get-in mem [:mem/stream :xt/id]))))
                     first)
            roles (:mem/roles mem)]
        (handler (assoc ctx :stream stream :roles roles :mem mem)))
      {:status 303
       :headers {"location" "/app"}})))

(defn wrap-channel [handler]
  (fn [{:keys [biff/db user mem community path-params] :as ctx}]
    (let [channel (xt/entity db (parse-uuid (:chan-id path-params)))]
      (if (and (= (:chan/comm channel) (:xt/id community)) mem)
        (handler (assoc ctx :channel channel))
        {:status 303
         :headers {"Location" (str "/community/" (:xt/id community))}}))))

(def plugin
  {:routes ["" {:middleware [mid/wrap-signed-in]}
            ["/app"           {:get app}]
            ["/stream"        {:post new-stream}]
            ["/stream/:id"    {:middleware [wrap-stream]}
             ["" {:get stream}]
             ["/connect" {:get connect-stream}]
             ["/join" {:post join-stream}]]
            ["/community"     {:post new-community}]
            ["/community/:id" {:middleware [wrap-community]}
             [""      {:get community}]
             ["/join" {:post join-community}]
             ["/channel" {:post new-channel}]
             ["/channel/:chan-id" {:middleware [wrap-channel]}
              ["" {:get channel-page
                   :post new-message
                   :delete delete-channel}]
              ["/connect" {:get connect}]]]]
   :on-tx on-new-message})
