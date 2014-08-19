(ns cmr.system-int-test.search.granule-search-producer-granule-id-test
  "Integration tests for searching by producer granule id"
  (:require [clojure.test :refer :all]
            [cmr.system-int-test.utils.ingest-util :as ingest]
            [cmr.system-int-test.utils.search-util :as search]
            [cmr.system-int-test.utils.index-util :as index]
            [cmr.system-int-test.data2.collection :as dc]
            [cmr.system-int-test.data2.granule :as dg]
            [cmr.system-int-test.data2.core :as d]))

(use-fixtures :each (ingest/reset-fixture {"provguid1" "PROV1" "provguid2" "PROV2"}))

(deftest search-by-producer-granule-id
  (let [coll1 (d/ingest "PROV1" (dc/collection {}))
        coll2 (d/ingest "PROV2" (dc/collection {}))
        gran1 (d/ingest "PROV1" (dg/granule coll1 {:producer-gran-id "Granule1"}))
        gran2 (d/ingest "PROV1" (dg/granule coll1 {:producer-gran-id "Granule2"}))
        gran3 (d/ingest "PROV1" (dg/granule coll1 {:producer-gran-id "SpecialOne"}))
        gran4 (d/ingest "PROV2" (dg/granule coll2 {:producer-gran-id "SpecialOne"}))
        gran5 (d/ingest "PROV2" (dg/granule coll2 {:producer-gran-id "Granule15"}))]

    (index/refresh-elastic-index)

    (testing "search by producer granule id."
      (are [items ids options]
           (let [params (merge {:producer-granule-id ids}
                               (when options
                                 {"options[producer-granule-id]" options}))]
             (d/refs-match? items (search/find-refs :granule params)))

           [] "NON_EXISTENT" {}
           [gran1] "Granule1" {}
           [gran1 gran2] ["Granule1", "Granule2"] {}
           ;search by producer granule id across different providers
           [gran3 gran4] "SpecialOne" {}

           ;; pattern
           [gran1 gran2 gran5] "Gran*" {:pattern true}
           [gran1 gran2] "Granule?" {:pattern true}

           ;; ignore case
           [] "granule1" {:ignore-case false}
           [gran1] "granule1" {:ignore-case true}
           [gran1] "granule1" {}))

    (testing "search by producer granule id with aql"
      (are [items ids options]
           (let [condition (merge {:ProducerGranuleID ids} options)]
             (d/refs-match? items (search/find-refs-with-aql :granule [condition])))

           [] "NON_EXISTENT" {}
           [gran1] "Granule1" {}
           [gran1 gran2] ["Granule1", "Granule2"] {}
           ;search by producer granule id across different providers
           [gran3 gran4] "SpecialOne" {}

           ;; pattern
           [gran1 gran2 gran5] "Gran%" {:pattern true}
           [gran1 gran2] "Granule_" {:pattern true}

           ;; ignore case
           [] "granule1" {:ignore-case false}
           [gran1] "granule1" {:ignore-case true}
           [gran1] "granule1" {}))))

