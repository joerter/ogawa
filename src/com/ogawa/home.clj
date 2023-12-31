(ns com.ogawa.home
  (:require [clj-http.client :as http]
            [com.biffweb :as biff]
            [com.ogawa.middleware :as mid]
            [com.ogawa.ui :as ui]
            [com.ogawa.settings :as settings]
            [rum.core :as rum]
            [xtdb.api :as xt]))

(def email-disabled-notice
  [:.text-sm.mt-3.bg-blue-100.rounded.p-2
   "Until you add API keys for Postmark and reCAPTCHA, we'll print your sign-up "
   "link to the console. See config.edn."])

(defn signup-form [{:keys [recaptcha/site-key params]}]
  (biff/form
   {:id "signup"
    :action "/auth/send-link"
    :hidden {:on-error "/"}
    :class "sm:max-w-xs w-full"}
   (biff/recaptcha-callback "submitSignup" "signup")
   [:input#email
    {:name "email"
     :type "email"
     :autocomplete "email"
     :placeholder "Enter your email address"
     :class '[border
              border-gray-300
              rounded
              w-full
              focus:border-teal-600
              focus:ring-teal-600]}]
   [:.h-3]
   [:button
    (merge (when site-key
             {:data-sitekey site-key
              :data-callback "submitSignup"})
           {:type "submit"
            :class '[bg-teal-600
                     hover:bg-teal-800
                     text-white
                     py-2
                     px-4
                     rounded
                     w-full
                     g-recaptcha]})
    "Join the waitlist"]
   (when-some [error (:error params)]
     [:<>
      [:.h-1]
      [:.text-sm.text-red-600
       (case error
         "recaptcha" (str "You failed the recaptcha test. Try again, "
                          "and make sure you aren't blocking scripts from Google.")
         "invalid-email" "Invalid email. Try again with a different address."
         "send-failed" (str "We weren't able to send an email to that address. "
                            "If the problem persists, try another address.")
         "There was an error.")]])))

(defn home-page [ctx]
  (ui/base
   (assoc ctx ::ui/recaptcha true)
   [:.bg-orange-50.flex.flex-col.flex-grow.items-center.p-3
    [:.h-12.grow]
    [:img.w-40 {:src "/img/eel.svg"}]
    [:.h-6]
    [:.text-2xl.sm:text-3xl.font-semibold.sm:text-center.w-full
     "The world's finest streaming platform"]
    [:.h-2]
    [:.sm:text-lg.sm:text-center.w-full
     "Easily stream life's important moments. Coming soon."]
    [:.h-6]
    (signup-form ctx)
    [:.h-12 {:class "grow-[2]"}]
    [:.text-sm biff/recaptcha-disclosure]
    [:.h-6]]))

(defn link-sent [{:keys [params] :as ctx}]
  (ui/page
   ctx
   [:h2.text-xl.font-bold "Check your inbox"]
   [:p "We've sent a sign-in link to " [:span.font-bold (:email params)] "."]))

(defn verify-email-page [{:keys [params] :as ctx}]
  (ui/page
   ctx
   [:h2.text-2xl.font-bold (str "Sign up for " settings/app-name)]
   [:.h-3]
   (biff/form
    {:action "/auth/verify-link"
     :hidden {:token (:token params)}}
    [:div [:label {:for "email"}
           "It looks like you opened this link on a different device or browser than the one "
           "you signed up on. For verification, please enter the email you signed up with:"]]
    [:.h-3]
    [:.flex
     [:input#email {:name "email" :type "email"
                    :placeholder "Enter your email address"}]
     [:.w-3]
     [:button.btn {:type "submit"}
      "Sign in"]])
   (when-some [error (:error params)]
     [:.h-1]
     [:.text-sm.text-red-600
      (case error
        "incorrect-email" "Incorrect email address. Try again."
        "There was an error.")])))

(defn signin-page [{:keys [recaptcha/site-key params] :as ctx}]
  (ui/page
   (assoc ctx ::ui/recaptcha true)
   (biff/form {:action "/auth/send-code"
               :id "signin"
               :hidden {:on-error "/signin"}}
              (biff/recaptcha-callback "submitSignin" "signin")
              [:div
               {:class "flex min-h-full flex-col justify-center px-6 py-12 lg:px-8"}
               [:div
                {:class "sm:mx-auto sm:w-full sm:max-w-sm"}
                [:img
                 {:class "mx-auto h-10 w-auto",
                  :src
                  "/img/eel.svg",
                  :alt "Ogawa"}]
                [:h2
                 {:class
                  "mt-5 text-center text-2xl font-bold leading-9 tracking-tight text-gray-900"}
                 "Sign in to " settings/app-name]]
               [:div
                {:class "mt-10 sm:mx-auto sm:w-full sm:max-w-sm"}
                [:div
                 [:label
                  {:for "email",
                   :class "block text-sm font-medium leading-6 text-gray-900"}
                  "Email address"]
                 [:div
                  {:class "mb-2"}
                  [:input
                   {:id "email",
                    :name "email",
                    :type "email",
                    :autocomplete "email",
                    :required "",
                    :class
                    "block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-indigo-600 sm:text-sm sm:leading-6"}]]]
                [:div
                 [:button
                  (merge (when site-key
                           {:data-sitekey site-key
                            :data-callback "submitSignin"})
                         {:type "submit"
                          :class "flex w-full justify-center rounded-md bg-indigo-600 px-3 py-1.5 text-sm font-semibold leading-6 text-white shadow-sm hover:bg-indigo-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600 g-recaptcha"})
                  "Sign in"]]
                (when-some [error (:error params)]
                  [:<>
                   [:.h-1]
                   [:.text-sm.text-red-600
                    (case error
                      "recaptcha" (str "You failed the recaptcha test. Try again, "
                                       "and make sure you aren't blocking scripts from Google.")
                      "invalid-email" "Invalid email. Try again with a different address."
                      "send-failed" (str "We weren't able to send an email to that address. "
                                         "If the problem persists, try another address.")
                      "invalid-link" "Invalid or expired link. Sign in to get a new link."
                      "not-signed-in" "You must be signed in to view that page."
                      "There was an error.")]])
                [:p
                 {:class "mt-10 text-center text-sm text-gray-500"}
                 "Not a member?"
                 [:a
                  {:href "#",
                   :class
                   "font-semibold leading-6 text-indigo-600 hover:text-indigo-500 ml-2"}
                  "Sign up now!"]]]
               biff/recaptcha-disclosure])))

(defn old-signin-page [{:keys [recaptcha/site-key params] :as ctx}]
  (ui/page
   (assoc ctx ::ui/recaptcha true)
   (biff/form
    {:action "/auth/send-code"
     :id "signin"
     :hidden {:on-error "/signin"}}
    (biff/recaptcha-callback "submitSignin" "signin")
    [:h2.text-2xl.font-bold "Sign in to " settings/app-name]
    [:.h-3]
    [:.flex
     [:input#email {:name "email"
                    :type "email"
                    :autocomplete "email"
                    :placeholder "Enter your email address"}]
     [:.w-3]
     [:button.btn.g-recaptcha
      (merge (when site-key
               {:data-sitekey site-key
                :data-callback "submitSignin"})
             {:type "submit"})
      "Sign in"]]
    (when-some [error (:error params)]
      [:<>
       [:.h-1]
       [:.text-sm.text-red-600
        (case error
          "recaptcha" (str "You failed the recaptcha test. Try again, "
                           "and make sure you aren't blocking scripts from Google.")
          "invalid-email" "Invalid email. Try again with a different address."
          "send-failed" (str "We weren't able to send an email to that address. "
                             "If the problem persists, try another address.")
          "invalid-link" "Invalid or expired link. Sign in to get a new link."
          "not-signed-in" "You must be signed in to view that page."
          "There was an error.")]])
    [:.h-1]
    [:.text-sm "Don't have an account yet? " [:a.link {:href "/"} "Sign up"] "."]
    [:.h-3]
    biff/recaptcha-disclosure
    email-disabled-notice)))

(defn enter-code-page [{:keys [recaptcha/site-key params] :as ctx}]
  (ui/page
   (assoc ctx ::ui/recaptcha true)
   (biff/form
    {:action "/auth/verify-code"
     :id "code-form"
     :hidden {:email (:email params)}}
    (biff/recaptcha-callback "submitCode" "code-form")
    [:div [:label {:for "code"} "Enter the 6-digit code that we sent to "
           [:span.font-bold (:email params)]]]
    [:.h-1]
    [:.flex
     [:input#code {:name "code" :type "text"}]
     [:.w-3]
     [:button.btn.g-recaptcha
      (merge (when site-key
               {:data-sitekey site-key
                :data-callback "submitCode"})
             {:type "submit"})
      "Sign in"]])
   (when-some [error (:error params)]
     [:.h-1]
     [:.text-sm.text-red-600
      (case error
        "invalid-code" "Invalid code."
        "There was an error.")])
   [:.h-3]
   (biff/form
    {:action "/auth/send-code"
     :id "signin"
     :hidden {:email (:email params)
              :on-error "/signin"}}
    (biff/recaptcha-callback "submitSignin" "signin")
    [:button.link.g-recaptcha
     (merge (when site-key
              {:data-sitekey site-key
               :data-callback "submitSignin"})
            {:type "submit"})
     "Send another code"])))

(def plugin
  {:routes [["" {:middleware [mid/wrap-redirect-signed-in]}
             ["/"                  {:get home-page}]]
            ["/link-sent"          {:get link-sent}]
            ["/verify-link"        {:get verify-email-page}]
            ["/signin"             {:get signin-page}]
            ["/verify-code"        {:get enter-code-page}]]})
