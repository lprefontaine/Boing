# Boing

Boing is a Java dependency injection tool written in Clojure. The main motivation behind
this initiative was to get away from Spring beans and many of the dependencies that it
carries in our software. Hence the name... if you have better suggestions, let us know.

##Why did we created this ?

We have some Java/Spring legacy code that we want to move to Clojure.
However some Java libraries like ORMs, ... require Java centric initializations.
We want to keep dependency injection for these initializations and avoid
having to recode them one by one.
They will still be needed after we move our Java code base to Clojure.

## Roadmap

We think that DI the way Boing implements it light weight enough such that
it can be generalized to interface Clojure with other Java frameworks simple
or complex.

We will see in the following weeks/months how far we can get with this with 
various examples applied in various spots in our code.

The next step(s) we envisionned:

- Experiment with other Java centric stuff that will not have equivalents
  in Clojure for a while. Suggestions are welcomed.

##Documentation

Please refer to the Wiki of the project at:
https://github.com/lprefontaine/Boing/wiki

##Examples

The examples folder contains presently one comparative example between Spring
and Boing. This one is complex and comes from some of our software use of
Spring beans. Other examples will follow.

##License

Copyright (C) 2011 Luc Préfontaine, SoftAddicts Inc.

Distributed under the Eclipse Public License, the same as Clojure
uses. 
