# Telegram Forwarder — Aplikasi Android

Aplikasi Android sederhana untuk **membaca pesan yang masuk ke satu bot Telegram**
lalu **meneruskannya ke tujuan Telegram lain** (chat / grup / channel).
Forwarder berjalan sebagai *foreground service* dengan notifikasi tetap.

## Tampilan & cara kerja

- Layar berisi: kolom token bot sumber, token bot pengirim (opsional), tujuan
  (chat id / @username), tombol **Mulai** dan **Berhenti**, status, dan log.
- Saat ditekan **Mulai**, service mulai mendengarkan pesan masuk dan
  meneruskannya. Notifikasi tetap muncul selama berjalan.
- Kalau bot sumber = bot pengirim, semua jenis konten disalin utuh (copyMessage).
  Kalau berbeda, pesan dikirim ulang sebagai teks.

## ⚠️ Batasan penting di HP

Android membatasi aplikasi latar belakang demi baterai. Agar forwarder tidak
mati sendiri:

- Biarkan notifikasi "Telegram Forwarder aktif" tetap ada (jangan di-swipe).
- Matikan **optimasi baterai** untuk aplikasi ini
  (Pengaturan → Aplikasi → TG Forwarder → Baterai → Tidak dibatasi).
- Sebagian HP (Xiaomi, Oppo, Vivo, Huawei) tetap agresif mematikan aplikasi —
  cari opsi "Autostart" / "Jalankan di latar belakang" dan izinkan.
- Forwarder hanya aktif selama HP menyala. Untuk pemakaian 24 jam yang andal,
  server tetap pilihan terbaik.

## Cara build jadi APK

Aplikasi ini berupa **kode sumber**. Untuk menjadikannya APK:

### 1. Pasang Android Studio
Unduh dari https://developer.android.com/studio dan pasang.

### 2. Buka proyek
- Buka Android Studio → **Open** → pilih folder `TelegramForwarderApp`.
- Android Studio akan otomatis melengkapi Gradle wrapper dan mengunduh
  dependensi (butuh koneksi internet, beberapa menit pada kali pertama).
- Jika diminta memperbarui versi Gradle/plugin, terima saja.

### 3. Build APK
- Menu **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
- Setelah selesai, klik **locate** untuk menemukan file `app-debug.apk`
  (biasanya di `app/build/outputs/apk/debug/`).

### 4. Pasang ke HP
- Salin `app-debug.apk` ke HP, buka, lalu pasang.
- Izinkan "Instal dari sumber tidak dikenal" bila diminta.
- Saat pertama dibuka, izinkan notifikasi.

## Menyiapkan bot & tujuan

1. Buat bot di **@BotFather** → `/newbot` → salin tokennya.
2. Untuk tahu **chat id** tujuan: isi token sumber, tekan Mulai, lalu kirim
   pesan ke bot. Untuk grup/channel, jadikan bot anggota/admin dulu;
   id grup/channel biasanya angka negatif (mis. `-1001234567890`).

## Catatan
- `applicationId` saat ini `com.example.tgforwarder`. Boleh diganti di
  `app/build.gradle` dan `AndroidManifest`/paket bila mau.
- Satu bot tidak boleh dipakai dua proses `getUpdates` sekaligus, dan tidak
  boleh sedang memakai webhook.
- Jaga kerahasiaan token.
