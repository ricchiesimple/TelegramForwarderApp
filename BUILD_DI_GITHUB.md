# Build APK tanpa Android Studio (lewat GitHub)

Server GitHub yang akan mengkompilasi APK untukmu. Kamu hanya perlu akun GitHub.

## Langkah

1. **Buat repo baru** di https://github.com/new (boleh private). Beri nama bebas.

2. **Unggah seluruh isi folder `TelegramForwarderApp`** ke repo itu.
   - Cara termudah tanpa Git: di halaman repo, klik **Add file → Upload files**,
     lalu seret semua file & folder (termasuk folder `.github`). Commit.
   - Pastikan struktur folder tetap sama (folder `app`, `.github/workflows`, dst.).

3. **Tunggu build berjalan.** Buka tab **Actions** di repo. Workflow "Build APK"
   akan jalan otomatis. Butuh beberapa menit pada kali pertama.

4. **Unduh APK.** Setelah build sukses (tanda centang hijau), klik pada run-nya,
   gulir ke bagian **Artifacts**, unduh **app-debug-apk**. Di dalamnya ada
   `app-debug.apk`.

5. **Pasang ke HP.** Salin APK ke HP Android, buka, pasang. Izinkan "instal dari
   sumber tidak dikenal" dan izin notifikasi bila diminta.

## Kalau build gagal

- Buka tab Actions → klik run yang merah → lihat langkah yang error untuk pesannya.
- Penyebab umum: ada file yang tidak ikut terunggah, atau struktur folder berubah.
  Pastikan `app/build.gradle`, `settings.gradle`, dan folder `app/src` ada.

## Catatan
- APK ini versi **debug** (untuk dipakai sendiri). Cukup untuk kebutuhanmu.
- Build di GitHub gratis untuk repo publik; repo private juga punya kuota
  gratis bulanan yang besar.
