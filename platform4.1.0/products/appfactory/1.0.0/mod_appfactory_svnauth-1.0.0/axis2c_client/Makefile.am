noinst_LTLIBRARIES =libappfactory_svnauth.la
libappfactory_svnauth_la_SOURCES =  appfactory_svnauth_stub.c \
                 appfactory_svnauth_client.c 
libappfactory_svnauth_la_LIBADD   =  \
                -L$(AXIS2C_HOME)/lib \
					-laxutil \
                    -laxis2_axiom \
                    -laxis2_engine \
                    -laxis2_parser \
                    -lpthread \
                    -laxis2_http_sender \
                    -laxis2_http_receiver \
                    $(GUTHTHILA_LIBS)

AM_CPPFLAGS = -I$(AXIS2C_HOME)/include/axis2-1.6.0 
