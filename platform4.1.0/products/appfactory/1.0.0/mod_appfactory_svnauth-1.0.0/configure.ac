AC_INIT([mod_appfactory_svnauth], [1.0.0], [support@wso2.com])
AM_INIT_AUTOMAKE([-Wall -Werror foreign])
AC_PROG_CC
AC_PROG_LIBTOOL
AC_CONFIG_HEADERS([config.h])
AC_CONFIG_FILES([
    Makefile
    apache_module/Makefile
    axis2c_client/Makefile
])
AC_CONFIG_MACRO_DIR([m4])

AC_CHECK_LIB([apr-1], [apr_pool_pre_cleanup_register], [], [AC_MSG_ERROR([apr_pool_pre_cleanup_register was not found in libapr-1])])

AC_CHECK_FILE([/usr/local/apache2/include/httpd.h], [APACHE_INCLUDE_DIR="/usr/local/apache2/include"], [
    AC_CHECK_FILE([/usr/include/httpd/httpd.h], [APACHE_INCLUDE_DIR="/usr/include/httpd"], [
        AC_CHECK_FILE([/usr/include/apache2/httpd.h], [APACHE_INCLUDE_DIR="/usr/include/apache2"], [
            AC_MSG_ERROR([Could not locate Apache include directory])
        ])
    ])
])
AC_SUBST([APACHE_INCLUDE_DIR])

AC_PATH_PROG([HTTPD], [httpd], , [/usr/local/apache2/bin:/usr/sbin])
APACHE_BIN_DIR=`AS_DIRNAME($HTTPD)`
AC_SUBST([APACHE_BIN_DIR])

        AC_CHECK_FILE([/usr/lib/httpd/modules/mod_dav.so], [APACHE_MODULES_DIR="/usr/lib/httpd/modules"], [
            AC_CHECK_FILE([/usr/lib/apache2/modules/mod_dav.so], [APACHE_MODULES_DIR="/usr/lib/apache2/modules"], [
                AC_MSG_ERROR([Could not locate Apache modules directory])
            ])
        ])


AC_SUBST([APACHE_MODULES_DIR])

AC_CHECK_FILE([/usr/local/apache2/conf/httpd.conf], [HTTPD_CONF="/usr/local/apache2/conf/httpd.conf"], [
    AC_CHECK_FILE([/etc/httpd/conf/httpd.conf], [HTTPD_CONF="/etc/httpd/conf/httpd.conf"], [
        AC_CHECK_FILE([/etc/apache2/httpd.conf], [HTTPD_CONF="/etc/apache2/httpd.conf"], [
            AC_MSG_ERROR([Could not locate Apache configuration file])
        ])
    ])
])
AC_SUBST([HTTPD_CONF])

AC_CHECK_FILE([/usr/local/apache2/bin/apxs], [APXS="/usr/local/apache2/bin/apxs"], [
    AC_CHECK_FILE([/usr/sbin/apxs], [APXS="/usr/sbin/apxs"], [
        AC_CHECK_FILE([/usr/bin/apxs2], [APXS="/usr/bin/apxs2"], [
            AC_MSG_ERROR([Could not locate Apache apxs binary])
        ])
    ])
])
AC_SUBST([APXS])

AC_CHECK_FILE([/usr/local/apache2/bin/apachectl], [APACHECTL="/usr/local/apache2/bin/apachectl"], [
    AC_CHECK_FILE([/usr/sbin/apache2ctl], [APACHECTL="/usr/sbin/apache2ctl"], [
        AC_CHECK_FILE([/usr/sbin/apachectl], [APACHECTL="/usr/sbin/apachectl"], [
            AC_MSG_ERROR([Could not locate Apache apachectl binary])
        ])
    ])
])
AC_SUBST([APACHECTL])

AC_OUTPUT
