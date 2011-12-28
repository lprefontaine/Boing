### Upcoming 1.2.2/1.3 release (target date December 2011)

* Added suport for Clojure 1.3
* Create two releases from a single source base, one supporting Clojure 1.2, the other supporting Clojure 1.3. This is an interim solution to be phased out later in 2012.

### 1.2.1 (2011-06-28)

* Added debug mode to trace bean instantiations, equally usable from the Java API
* Fixed a singleton post fn bug, the fn was called every time a singleton was requested instead of only once
* Fixed resource load issue when using url like file://.
* Fixed resource enum, nil values were added to the result list

### 1.2 (2011-06-17)

* Added a Java API to load bean definitions written in Clojure and to instantiate beans
* Fixed overrides when using constructor values. Overrides were not
  executed.

### 1.1 (2011-05-14)

* Fixed a bug with singleton using factories, was crashing after first instantiation (class cast exception)
* Optimized the code with transients
* Added the possibility to instantiate singletons at bean definition time instead of lazy initialization
* Added the ability to alias property names across all bean definitions
* Allow a caller to add argument auto-promotions
* Added resource input streams

### 1.0 (2011-04-27)

* First version




