#!/bin/bash

CSV_FILE="test_messages.csv"

# Rastgele 100 satırı al (başlığı atla), ardından her satır için işle
tail -n +2 "$CSV_FILE" | shuf | head -n 100 | while IFS=';' read -r number message ; do
  echo "Gönderiliyor: $number -> $message"
  adb emu sms send "$number" "$message"
  sleep 0.1
done
