(ns com.ogawa.app
  (:require
   [com.biffweb :as biff :refer [q]]
   [com.ogawa.middleware :as mid]
   [com.ogawa.ui :as ui]
   [xtdb.api :as xt]))

(defn app [ctx]
  (ui/app-page
   ctx
   [:p "Select a community, or create a new one."]))

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
    (message-view msg)))

(defn channel-page [{:keys [biff/db community channel] :as ctx}]
  (let [msgs (q db
                '{:find (pull msg [*])
                  :in [channel]
                  :where [[msg :msg/channel channel]]}
                (:xt/id channel))]
    (ui/app-page
     ctx
     [:.border.border-neutral-600.p-3.bg-white.grow.flex-1.overflow-y-auto#messages
      {:_ "on load or newMessage set my scrollTop to my scrollHeight"}
      (map message-view (sort-by :msg/created-at msgs))]
     [:.h-3]
     (biff/form
      {:hx-post (str "/community/" (:xt/id community)
                     "/channel/" (:xt/id channel))
       :hx-target "#messages"
       :hx-swap "beforeend"
       :_ (str "on htmx:afterRequest"
               " set <textarea/>'s value to ''"
               " then send newMessage to #messages")
       :class "flex"}
      [:textarea.w-full#text {:name "text"}]
      [:.w-2]
      [:button.btn {:type "submit"} "Send"]))))

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
            ["/community"     {:post new-community}]
            ["/community/:id" {:middleware [wrap-community]}
             [""      {:get community}]
             ["/join" {:post join-community}]
             ["/channel" {:post new-channel}]
             ["/channel/:chan-id" {:middleware [wrap-channel]}
              ["" {:get channel-page
                   :post new-message
                   :delete delete-channel}]]]]})
