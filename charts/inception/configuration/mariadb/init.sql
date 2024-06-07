CREATE DATABASE inception DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
CREATE USER '{{ .Values.mariadb.auth.username }}'@'localhost' IDENTIFIED BY '{{ .Values.mariadb.auth.password }}';
GRANT ALL PRIVILEGES ON inception.* TO '{{ .Values.mariadb.auth.username }}'@'localhost';
FLUSH PRIVILEGES;