#! /bin/sh

# assuming this action was already performed before to run this install.sh script
# git clone https://github.com/alex-estela/peerframe

cd /home/pi

sudo apt-get update

# sudo apt-get upgrade -y
# sudo apt-get dist-upgrade –y

sudo apt-get install -y tint2 matchbox-window-manager postgresql imagemagick

wget http://www-us.apache.org/dist/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz
sudo tar -xzvf apache-maven-3.3.9-bin.tar.gz
echo 'export M2_HOME=/home/pi/apache-maven-3.3.9
export PATH=$PATH:$M2_HOME/bin ' >> /home/pi/maven.sh
sudo mv /home/pi/maven.sh /etc/profile.d/maven.sh
. /etc/profile.d/maven.sh

git clone git://github.com/rg3/youtube-dl
sudo ln -s ~/youtube-dl/youtube_dl/__main__.py /usr/bin/youtube-dl

wget http://steinerdatenbank.de/software/kweb-1.7.0.tar.gz
tar -xzf kweb-1.7.0.tar.gz
cd kweb-1.7.0
./debinstall

echo 'matchbox-window-manager -use_titlebar no -use_cursor no &
xset s noblank
xset -dpms
xset s off
kweb3 -KAHZJEobhrp+-zgtjnediwxyqcf "file:///home/pi/peerframe/target/classes/static/index.html" ' >> /home/pi/kiosk
chmod +x /home/pi/kiosk

sudo sed -i '/fi/axinit /home/pi/kiosk' /etc/rc.local

sudo sed -i 's/allowed_users=console/allowed_users=anybody/g' /etc/X11/Xwrapper.config

sudo sed -i '/README/agpu_mem=16s' /boot/config.txt

# Config spéciale HDML PI 7
sudo sed -i '/README/alcd_rotate=2' /boot/config.txt

sudo ln -s /usr/bin/convert /usr/bin/convert-peerframe

cd /home/pi/peerframe
mkdir tmp
mvn clean install spring-boot:repackage

echo '#! /bin/sh

### BEGIN INIT INFO
# Provides:          peerframe-backend
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
ARGS="-Dconfig=$PEERFRAME_DIR/target/classes/inflector.yaml -jar $PEERFRAME_DIR/target/peerframe.jar"

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

sudo runuser -l postgres -c "psql -c \"CREATE USER peerframe WITH PASSWORD 'peerframe';\""
sudo runuser -l postgres -c "psql -c \"CREATE DATABASE peerframe OWNER peerframe;\""

sudo raspi-config --expand-rootfs

# enable console auto login (emulate raspi-config boot behaviour B2)
sudo systemctl set-default multi-user.target
sudo ln -fs /etc/systemd/system/autologin@.service /etc/systemd/system/getty.target.wants/getty@tty1.service

sudo reboot