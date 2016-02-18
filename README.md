easy-validate-aip
=================

Validate one or more AIPs in (dark) archival storage

SYNOPSIS
--------

    easy-validate-aip <aip-directory>
    easy-validate-aip <Fedora service URL> <aip-base-directory>
    
DESCRIPTION
-----------
AIPs in dark archive are stored as [bag]s. These bags contain the data files (Content Data Objects) and descriptive metadata. Since the
bags are valid, they contain checkums for all the data and metadata files. `easy-validate-aip` validates the bag(s) and reports whether
they are valid. 

There are two modes of execution:

1. Validate a single AIP
2. Validata all the AIPs registered in an EASY Fedora 3.x repository

### Validating a single AIP
When validating a single AIP the directory containing the AIP is passed as an argument. The program reports back a message stating
whether the bag is valid.

### Validating all the AIPs in an EASY Fedora 3.x repository
To validate all the AIPs registered in an EASY Fedora 3.x repository the service URL of the repository and the base directory containing
all the AIPs are passed as arguments. The program queries Fedora's Resource Index for all the datasets that have the relation `http://dans.knaw.nl/ontologies/relations#storedInDarkArchive` set to `true`. From these datasets the [URN:NBN] identifier is retrieved.
This identifier is used to find the AIP directory in the AIP base directory. 


ARGUMENTS
---------



INSTALLATION AND CONFIGURATION
------------------------------



BUILDING FROM SOURCE
--------------------
Prerequisites:

* Java 8 or higher
* Maven 3.3.3 or higher
 
Steps:

        git clone https://github.com/DANS-KNAW/easy-validate-aip.git
        cd easy-validate-aip
        mvn install









[bag]: https://tools.ietf.org/html/draft-kunze-bagit-12
