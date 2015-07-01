(ns cmr.common.test.validations.json-schema
  "Tests to verify JSON schema validation."
  (:require [clojure.test :refer :all]
            [cheshire.core :as json]
            [cmr.common.validations.json-schema :as js]
            [cmr.common.test.test-util :as tu]
            [cmr.common.util :as u])
  (:import com.github.fge.jsonschema.core.exceptions.InvalidSchemaException))

(def sample-json-schema
  "Schema to test validation against"
  (json/encode {"$schema" "http://json-schema.org/draft-04/schema#"
                "title" "The title"
                "description" "A description"
                "type" "object"
                "additionalProperties" false
                "properties" {"foo" {"oneOf" [{ "type" "string"}
                                              { "type" "integer"}]}
                              "bar" {"type" "boolean"}
                              "alpha" {"type" "string"}}
                "required" ["bar"]}))

(deftest json-schema-validations-test
  (testing "Validation failures"
    (u/are2 [invalid-json errors]
            (tu/assert-exception-thrown-with-errors
              :bad-request
              errors
              (js/validate-against-json-schema sample-json-schema invalid-json))

            "Missing required property"
            {"alpha" "omega"}
            [" object has missing required properties ([\"bar\"])"]

            "Wrong type for property with single type"
            {"alpha" 17 "bar" true}
            ["/alpha instance type (integer) does not match any allowed primitive type (allowed: [\"string\"])"]

            "Wrong type for property which can have multiple types"
            {"foo" true "bar" true}
            ["/foo instance failed to match exactly one schema (matched 0 out of 2)"
             "/foo instance type (boolean) does not match any allowed primitive type (allowed: [\"string\"])"
             "/foo instance type (boolean) does not match any allowed primitive type (allowed: [\"integer\"])"]

            "Unknown property"
            {"bad-property" "bad-value" "bar" true}
            [" object instance has properties which are not allowed by the schema: [\"bad-property\"]"]))

  (testing "Invalid schema - description is a string field"
    (is (thrown-with-msg?
          InvalidSchemaException
          #"value has incorrect type \(found array, expected one of \[string\]\)"
          (js/validate-against-json-schema (json/encode {"$schema" "http://json-schema.org/draft-04/schema#"
                                                         "title" "The title"
                                                         "description" ["A description" "B description"]}) {}))))
  (testing "Valid json"
    (is (nil? (js/validate-against-json-schema sample-json-schema {"bar" true})))))


(comment
  (def query-schema (slurp (clojure.java.io/resource "schema/JSONQueryLanguage.json")))
  (js/validate-against-json-schema query-schema {"provider" {"prov" "PROV1"
                                                             "123" "567"
                                                             "value" "44"}})

  (js/validate-against-json-schema query-schema {"and" [{"provider" {"prov" "PROV1"
                                                                     "123" "567"
                                                                     "value" "44"}
                                                         "bad" "key"}]})
  )
