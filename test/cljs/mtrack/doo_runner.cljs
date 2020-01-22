(ns mtrack.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [mtrack.core-test]))

(doo-tests 'mtrack.core-test)

