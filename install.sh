#! /bin/sh

# assuming this action was already performed before to run this install.sh script
# git clone https://github.com/alex-estela/peerframe

cd /home/pi

sudo apt-get install -y postgresql imagemagick unclutter matchbox-keyboard

sudo ln -s /usr/bin/convert /usr/bin/convert-peerframe

sudo runuser -l postgres -c "psql -c \"CREATE USER peerframe WITH PASSWORD 'peerframe';\""
sudo runuser -l postgres -c "psql -c \"CREATE DATABASE peerframe OWNER peerframe;\""

wget http://www-us.apache.org/dist/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz
sudo tar -xzvf apache-maven-3.3.9-bin.tar.gz
echo 'export M2_HOME=/home/pi/apache-maven-3.3.9
export PATH=$PATH:$M2_HOME/bin ' >> /home/pi/maven.sh
sudo mv /home/pi/maven.sh /etc/profile.d/maven.sh
. /etc/profile.d/maven.sh

cd /home/pi/peerframe
mkdir tmp
mkdir data
mvn clean package

echo '#! /bin/sh

### BEGIN INIT INFO
# Provides:          peerframe
# Required-Start:    $local_fs $network $remote_fs
# Required-Stop:     $local_fs $network $remote_fs
# Should-Start:      $NetworkManager
# Should-Stop:       $NetworkManager
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: init script
# Description:       init script
### END INIT INFO

PEERFRAME_DIR="/home/pi/peerframe"
PID_FILE=/tmp/peerframe.pid
ARGS="-jar $PEERFRAME_DIR/target/peerframe.jar"

case "$1" in
  start)
    start-stop-daemon --start --quiet --background --make-pidfile --pidfile $PID_FILE --chdir $PEERFRAME_DIR --chuid pi --exec "/usr/bin/java" -- $ARGS
    ;;
  stop)
    start-stop-daemon --stop --quiet --pidfile $PID_FILE
    ;;
  *)
    echo "Usage: sudo /etc/init.d/peerframe {start|stop}"
    exit 1
    ;;
esac

exit 0' >> /home/pi/initd-peerframe
sudo mv /home/pi/initd-peerframe /etc/init.d/peerframe
sudo chmod +x /etc/init.d/peerframe
sudo update-rc.d peerframe defaults

sudo sed -i '/README/alcd_rotate=2' /boot/config.txt
sudo sed -i '/README/aavoid_warnings=1' /boot/config.txt
sudo sed -i '/README/adisable_splash=1' /boot/config.txt

sudo sed -i 's/XKBLAYOUT="[^"]*"/XKBLAYOUT="fr"/' /etc/default/keyboard

sudo rm -f /etc/profile.d/sshpwd.sh
sudo rm -f /etc/xdg/lxsession/LXDE-pi/sshpwd.sh
sudo su -c "echo 'pi:peer2peer' | chpasswd"

sudo mv /usr/share/plymouth/themes/pix/splash.png /usr/share/plymouth/themes/pix/splash.png.old
sudo su -c "echo `sed '$s/$/ logo.nologo/' /boot/cmdline.txt` > /boot/cmdline.txt"

sudo sed -i '/fi/axinit /home/pi/kiosk.sh' /etc/rc.local

echo '#!/bin/bash

xset s noblank
xset -dpms
xset s off
unclutter &

sudo -u pi /usr/bin/chromium-browser --kiosk --disable-infobars --disable-session-crashed-bubble --noerrdialogs --incognito --window-position=0,0 --window-size=800,600 file:///home/pi/peerframe/target/classes/static/index.html
' >> /home/pi/kiosk.sh

echo '@xset s 0 0
@xset s noblank
@xset s noexpose
@xset dpms 0 0 0
@unclutter
' >> /home/pi/.config/lxsession/LXDE-pi/autostart

# sudo touch /boot/ssh

# cp /etc/wpa_supplicant/wpa_supplicant.conf /home/pi/wpa_supplicant.conf
# echo 'network={
#         ssid="TBD"
#         psk="TBD"
# }' >> /home/pi/wpa_supplicant.conf
# sudo mv -f /home/pi/wpa_supplicant.conf /etc/wpa_supplicant/wpa_supplicant.conf

echo '#!/bin/bash
wifissid=$1
wifikey=$2
sudo sed -i 's/ssid="[^"]*"/ssid="'"$wifissid"'"/' /etc/wpa_supplicant/wpa_supplicant.conf
sudo sed -i 's/psk="[^"]*"/psk="'"$wifikey"'"/' /etc/wpa_supplicant/wpa_supplicant.conf
echo "Wifi update script completed"
' >> /home/pi/updatewifi
sudo chmod +x /home/pi/updatewifi
sudo mv /home/pi/updatewifi /usr/bin

echo '#!/bin/bash
cd /home/pi/peerframe
git reset --hard HEAD
git pull
. /etc/profile.d/maven.sh
mvn clean package 
sudo reboot
' >> /home/pi/upgradedevice
sudo chmod +x /home/pi/upgradedevice
sudo mv /home/pi/upgradedevice /usr/bin

sudo reboot