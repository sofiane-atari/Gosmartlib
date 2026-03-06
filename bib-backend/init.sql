-- Allow root to connect from any host (needed for local dev connecting through Docker bridge)
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;
