(ns com.ogawa.ui.components
  (:require [com.ogawa.ui.icons :as icons]
            [com.biffweb :as biff]))

(defn old-profile [user]
  [:.text-sm (:user/email user) " | "
   (biff/form
    {:action "/auth/signout"
     :class "inline"}
    [:button.text-teal-600.hover:text-teal-800 {:type "submit"}
     "Sign out"])])

(defn- profile-dropdown-menu []
  [:div {:class "absolute right-0 z-10 mt-2.5 w-32 origin-top-right rounded-md bg-white py-2 shadow-lg ring-1 ring-gray-900/5 focus:outline-none"
         :role "menu"
         :aria-orientation "vertical"
         :aria-labelledby "user-menu-button"
         :tabindex "-1"
         :x-show "profileDropdownMenuOpen"
         :x-transition ""}
   (biff/form
    {:action "/auth/signout"
     :class "inline"}
    [:button {:type "submit"
              :class "block px-3 py-1 text-sm leading-6 text-gray-900"
              :role "menuitem"
              :tabindex "-1"
              :id "user-menu-item-1"}
     "Sign out"])])

(def profile-dropdown
  [:div {:class "relative"
         :x-data "{profileDropdownMenuOpen: false}"}
   [:button {:type "button"
             :class "-m-1.5 flex items-center p-1.5"
             :id "user-menu-button"
             :aria-expanded "false"
             :aria-haspopup "true"
             "@click" "profileDropdownMenuOpen = ! profileDropdownMenuOpen"}
    [:span {:class "sr-only"} "Open user menu"]
    [:svg
     {:class "w-8 h-8 bg-indigo-600 text-white rounded-full",
      :xmlns "http://www.w3.org/2000/svg",
      :viewBox "0 0 24 24"}
     [:text
      {:x "50%",
       :y "55%",
       :dominant-baseline "middle",
       :text-anchor "middle",
       :font-size "12",
       :fill "white"}
      "SJ"]]
    [:span {:class "hidden lg:flex lg:items-center"}
     [:span {:class "ml-4 text-sm font-semibold leading-6 text-gray-900"
             :aria-hidden "true"} "Steve Jobs"]
     icons/chevron-down]]
   (profile-dropdown-menu)])
