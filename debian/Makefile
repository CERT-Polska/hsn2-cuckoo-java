#
# This Makefile is responsoble for build debian package.
# 4 variables must be provided:
#   HSN2_VER=2.X
#   BUILD_NUMBER=999
#   DEBIAN_DIST=(experimental|2.X)

DEBIAN_DIST=experimental
HSN2_COMPONENT=cuckoo-java

all:	hsn2-cuckoo-java

clean:	hsn2-cuckoo-java-clean

PKG=hsn2-$(HSN2_COMPONENT)_$(HSN2_VER)-$(BUILD_NUMBER)_all
hsn2-cuckoo-java: hsn2-cuckoo-java-clean
	mkdir -p $(PKG)/DEBIAN
	mkdir -p $(PKG)/etc/init.d
	mkdir -p $(PKG)/opt/hsn2/$(HSN2_COMPONENT)
	tar -zxf ../target/hsn2-cuckoo-java-*.tar.gz -C $(PKG)/opt/hsn2/$(HSN2_COMPONENT)
	cp cuckoo-java.initd $(PKG)/etc/init.d/hsn2-$(HSN2_COMPONENT)
	cp control ${PKG}/DEBIAN
	sed -i "s/{VER}/${HSN2_VER}-${BUILD_NUMBER}/" $(PKG)/DEBIAN/control
	sed -i "s/{DEBIAN_DIST}/${DEBIAN_DIST}/" $(PKG)/DEBIAN/control
	fakeroot dpkg -b $(PKG)

hsn2-cuckoo-java-clean:
	rm -rf hsn2-$(HSN2_COMPONENT)*
