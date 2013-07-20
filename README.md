tagrelator
==========
This is the result of a programming project in university.

the approach is taken from a paper that can be found here.
http://www.site.uottawa.ca/~mdislam/publications/LREC_06_242.pdf

The aim is to compute a score for a pair of words that expresses their similarity in meaning.
For a pair like cat - tiger the score should be rather high, meaning that these two words describe similar concepts,
whereas for a pair like cat - car the score should be quite low.

The score is computed automatically, leveraging the statistical analysis of large collections of written text.

The hypothesis behind the score is, that words similar in meaning also have similar contexts. the contexts are 
the words that stand before after their occurences in written text. Thats why a large collection of text is needed,
to find occurences of target words and look at their contexts. To the data collected from these contexts, the two measures
PMI-Pointwise mutual information and SOC-PMI - Second Order Cooccurence PMI are applied to compute the similarity score 
for a pair of words.

For the programming project the approach from the paper was implemented. Furthermore it was ported to the new domain
of Flickr Photo Tags. This mainly consisted in implementing data collection on Flickr and storage.


Java Classes
============

The project was developed and used as a whole and though i tried to implement the classes in a way that some can be used
individually, testing that is planned for future.

The Flickr data collector uses the flickrapi-1.2
http://sourceforge.net/projects/flickrj/files/

The reader classes for the OANC corpus uses the commons-compress libraries
http://commons.apache.org/proper/commons-compress/

the OANC can be found here
http://www.americannationalcorpus.org/OANC/index.html

It turned out that it is too small for the purposes at hand, so the BNC corpus was put in use.
Sadly the BNC corpus is not available for free.

the javadoc is in the doc directory
