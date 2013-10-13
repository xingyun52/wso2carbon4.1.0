<xsl:stylesheet version="2.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:fn="http://www.w3.org/2005/02/xpath-functions"
        xmlns:m0="http://services.samples"
        xmlns:ax21="http://services.samples/xsd"
        exclude-result-prefixes="m0 ax21 fn">
<xsl:output method="xml" omit-xml-declaration="yes" indent="yes"/>

<xsl:template match="/">
  <xsl:apply-templates select="//m0:return" />
</xsl:template>

<xsl:template match="m0:return">

<m:CheckPriceResponse xmlns:m="http://services.samples/xsd">
        <m:Code><xsl:value-of select="ax21:symbol"/></m:Code>
        <m:Price><xsl:value-of select="ax21:last"/></m:Price>
</m:CheckPriceResponse>

</xsl:template>
</xsl:stylesheet>