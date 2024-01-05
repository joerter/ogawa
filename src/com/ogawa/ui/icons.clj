(ns com.ogawa.ui.icons)

(def data
  {:x {:view-box "0 0 384 512", :path "M376.6 84.5c11.3-13.6 9.5-33.8-4.1-45.1s-33.8-9.5-45.1 4.1L192 206 56.6 43.5C45.3 29.9 25.1 28.1 11.5 39.4S-3.9 70.9 7.4 84.5L150.3 256 7.4 427.5c-11.3 13.6-9.5 33.8 4.1 45.1s33.8 9.5 45.1-4.1L192 306 327.4 468.5c11.3 13.6 31.5 15.4 45.1 4.1s15.4-31.5 4.1-45.1L233.7 256 376.6 84.5z"}})

(defn icon [k & [opts]]
  (let [{:keys [view-box path]} (data k)]
    [:svg.flex-shrink-0.inline
     (merge {:xmlns "http://www.w3.org/2000/svg"
             :viewBox view-box}
            opts)
     [:path {:fill "currentColor"
             :d path}]]))

(def chevron-down
  [:svg {:class "ml-2 h-5 w-5 text-gray-400"
         :viewBox "0 0 20 20"
         :fill "currentColor"
         :aria-hidden "true"}
   [:path {:fill-rule "evenodd"
           :d "M5.23 7.21a.75.75 0 011.06.02L10 11.168l3.71-3.938a.75.75 0 111.08 1.04l-4.25 4.5a.75.75 0 01-1.08 0l-4.25-4.5a.75.75 0 01.02-1.06z"
           :clip-rule "evenodd"}]])
