(ns com.ogawa.schema)

(def schema
  {:user/id :uuid
   :user    [:map {:closed true}
             [:xt/id          :user/id]
             [:user/email     :string]
             [:user/joined-at inst?]]

   :comm/id   :uuid
   :community [:map {:closed true}
               [:xt/id      :comm/id]
               [:comm/title :string]]

   :stream/id :uuid
   :stream    [:map {:closed true}
               [:xt/id      :stream/id]
               [:stream/title :string]]

   :offer/id  :uuid
   :offer     [:map {:closed true}
               [:xt/id          :offer/id]
               [:offer/sdp      :string]
               [:offer/type     :string]
               [:offer/stream   :stream/id]
               [:offer/user     :user/id]]

   :mem/id     :uuid
   :membership [:map {:closed true}
                [:xt/id     :mem/id]
                [:mem/user  :user/id]
                [:mem/stream  :stream/id]
                [:mem/roles [:set [:enum :admin]]]]

   :chan/id :uuid
   :channel [:map {:closed true}
             [:xt/id      :chan/id]
             [:chan/title :string]
             [:chan/comm  :comm/id]]

   :msg/id  :uuid
   :message [:map {:closed true}
             [:xt/id          :msg/id]
             [:msg/mem        :mem/id]
             [:msg/text       :string]
             [:msg/channel    :chan/id]
             [:msg/created-at inst?]]})

(def plugin
  {:schema schema})
