lib_LTLIBRARIES =mod_appfactory_svnauth.la
mod_appfactory_svnauth_la_SOURCES = mod_appfactory_svnauth.c cache.c util.c
mod_appfactory_svnauth_la_LDFLAGS = -module
mod_appfactory_svnauth_la_LIBADD=-L$(top_builddir)/axis2c_client \
                                             -lappfactory_svnauth
AM_CFLAGS = `apr-1-config --cflags`
AM_CPPFLAGS = -I@APACHE_INCLUDE_DIR@ `apr-1-config --cppflags --includes` \
	  -I$(top_builddir)/axis2c_client \
          -I$(AXIS2C_HOME)/include/axis2-1.6.0
