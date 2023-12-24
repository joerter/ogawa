(ns com.ogawa.repl
  (:require [com.ogawa :as main]
            [com.biffweb :as biff :refer [q]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn get-context []
  (biff/assoc-db @main/system))

(defn add-fixtures []
  (biff/submit-tx (get-context)
                  (-> (io/resource "fixtures.edn")
                      slurp
                      edn/read-string)))

(defn seed-channels []
  (let [{:keys [biff/db] :as ctx} (get-context)]
    (biff/submit-tx ctx
                    (for [[mem chan] (q db
                                        '{:find [mem chan]
                                          :where [[mem :mem/comm comm]
                                                  [chan :chan/comm comm]]})]
                      {:db/doc-type :message
                       :msg/mem mem
                       :msg/channel chan
                       :msg/created-at :db/now
                       :msg/text (str "Seed message " (rand-int 1000))}))))

(comment

  ;; Call this in dev if you'd like to add some seed data to your database. If
  ;; you edit the seed data (in resources/fixtures.edn), you can reset the
  ;; database by running `rm -r storage/xtdb` (DON'T run that in prod),
  ;; restarting your app, and calling add-fixtures again.
  (add-fixtures)

  (defn get-secret [ctx k]

  (some-> (get {:postmark/api-key "1234abc"} :postmark/api-key)
          (System/getenv)
          not-empty))

  (let [{:keys [biff/db] :as ctx} (get-context)]
    (q db
       '{:find (pull user [*])
         :where [[user :user/email]]}))

  (sort (keys (get-context)))

  ;; Check the terminal for output.
  (biff/submit-job (get-context) :echo {:foo "bar"})
  (deref (biff/submit-job-for-result (get-context) :echo {:foo "bar"}))
  
  (seed-channels)

  (pr {:john "is cool"})
  (flush)
  
  (let [{:keys [biff/db] :as ctx} (get-context)]
    (q db
       '{:find (pull msg [*])
         :where [[msg :msg/text]]})))
