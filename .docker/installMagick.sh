# installs ImageMagick on system and solves the issue with PDF file converting
apt update && apt install imagemagick -y
sed -i_bak 's/rights="none" pattern="PDF"/rights="read | write" pattern="PDF"/' /etc/ImageMagick-6/policy.xml
echo "Installation of ImageMagick is completed"