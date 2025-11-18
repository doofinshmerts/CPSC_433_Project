#
# Package
#

NAMESPACE := schedulesearch
PACKAGE := $(NAMESPACE)

#
# Files
#
SRC_DIR := schedulesearch
SOURCES := $(wildcard $(SRC_DIR)/*.java)

BUILDDIR := build
EXECUTABLE := Build.jar

#
# Compilers
#

JC := javac
JCFLAGS := -d $(BUILDDIR)

JR := jar

#
# Build
#

$(BUILDDIR):
	mkdir -p $(BUILDDIR)

all: $(BUILDDIR)
	$(JC) $(JCFLAGS) $(SOURCES)
	$(JR) cfe $(BUILDDIR)/$(EXECUTABLE) $(PACKAGE).Main -C $(BUILDDIR) $(NAMESPACE)

clean:
	rm -r $(BUILDDIR)
