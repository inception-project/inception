CREATE DATABASE webanno DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ;

CREATE USER 'webanno'@'localhost' IDENTIFIED BY 't0t4llYSecreT';
GRANT ALL PRIVILEGES ON webanno.* TO 'webanno'@'localhost';
