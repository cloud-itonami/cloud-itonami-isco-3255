(ns physiotherapy-support.advisor
  "PhysiotherapySupportAdvisor — proposes a physiotherapy-support
  operation (assist a session, monitor a session, directly manipulate
  a patient's body, run an exercise involving fall risk) for a
  registered client. The advisor is swappable: `mock-advisor`
  (deterministic, default in dev/tests/CI) or `llm-advisor` (wraps a
  real `langchain.model/ChatModel`). Either way the advisor ONLY
  produces a PROPOSAL — it never writes to the store and has no notion
  of client provenance or direct-manipulation/fall-risk consequences;
  `physiotherapy-support.governor` is the independent system that
  decides whether the proposal may proceed, per the itonami actor
  pattern.

  A proposal is a map:
    {:op :assist|:monitor|:direct-physical-manipulation|:fall-risk-exercise
     :effect :propose        ; the advisor NEVER emits a raw store write
     :stake :low|:medium|:high
     :confidence 0.0-1.0
     :rationale str}
  LLM parse failures always yield `:confidence 0.0` (never fabricate
  confidence), which forces the governor to escalate/hold."
  (:require [clojure.string :as str]
            #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])))

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer
  "Deterministic mock inference: reads the request's declared op/stake
  straight through (a stand-in for what an LLM would extract from free
  text), with a stake-derived confidence."
  [_store {:keys [op stake] :as request}]
  {:op op
   :effect :propose
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed " (name op) " for client " (:client-id request))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a physiotherapy-support advisor. Given a support operation
   request, propose an :op, an honest :confidence (0.0-1.0), and a
   :stake (:low/:medium/:high). Never fabricate confidence you don't
   have.")

(defn- parse-proposal [content]
  (try
    (let [p (edn/read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  "Wraps a `langchain.model/ChatModel`. `gen-opts` is passed through to
  `model/-generate`. Kept decoupled from any concrete model so this ns
  has no hard dependency beyond `langchain.model`'s protocol."
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
