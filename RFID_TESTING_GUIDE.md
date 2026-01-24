# RFID Testing Guide - Chainway C72

## Quick Testing Checklist

### ✅ Pre-Testing Setup

1. **Device Requirements:**
   - Chainway C72 handheld device with UHF RFID module
   - RFID tags (UHF EPC Gen2 compatible)
   - Charged battery

2. **App Permissions:**
   - Check AndroidManifest.xml has necessary permissions
   - Grant runtime permissions if needed

3. **Build and Install:**
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

---

## Testing Sequence

### Test 1: Initialization ✅
**Location:** AssetsScreen.kt - Line ~150

**What happens:**
- `c72RfidReader.initialize()` called in `LaunchedEffect`
- Should complete without errors
- Check Logcat for: "UHF Reader initialized successfully"

**If it fails:**
- Check Logcat for specific error message
- Ensure device has RFID hardware
- Try restarting the device

---

### Test 2: Single Tag Read 📖
**Location:** AssetsScreen.kt - RFID Scan button

**Steps:**
1. Place RFID tag near device antenna (usually back of device)
2. Tap "RFID Scan" button in Assets screen
3. Tag should be detected within 1-2 seconds

**Expected Result:**
```
✅ Dialog shows: "RFID tag detected: [EPC_VALUE]"
✅ Asset matched or "Asset not found" message
```

**Troubleshooting:**
- Move tag closer to antenna
- Check tag is UHF compatible (860-960 MHz)
- Verify antenna is active (some devices have physical switch)

---

### Test 3: Write Tag ✍️
**Location:** AssetsScreen.kt - Write button in asset list

**Steps:**
1. Select an asset from list
2. Tap Write button
3. Place blank RFID tag near antenna
4. Confirm write operation

**Expected Result:**
```
✅ "RFID tag written successfully" message
✅ Tag now contains asset ID
```

**Data Format:**
- Asset ID padded to 6 digits: `000123`
- Written to EPC bank starting at word 2
- Hex encoded

**Troubleshooting:**
- Use new/blank tags for first test
- If tag has data, app will ask for confirmation to overwrite
- Some tags are read-only - check tag type

---

### Test 4: Bulk Inventory 📡
**Location:** AssetsScreen.kt - Bulk scan functionality

**Steps:**
1. Place multiple RFID tags in antenna range
2. Start bulk inventory scan
3. Keep tags in range for 3-5 seconds

**Expected Result:**
```
✅ Multiple tags detected
✅ Each tag EPC shown in list
✅ Sound/vibration feedback for each tag
```

**Troubleshooting:**
- Don't move tags during scan
- Maximum 50-100 tags at once for best performance
- Check antenna power settings if range is low

---

## Common Issues and Solutions

### Issue: "RFID hardware not found"
**Solutions:**
1. Verify device model is Chainway C72 with UHF RFID
2. Check if RFID module is enabled in device settings
3. Restart the device
4. Check DeviceAPI AAR is correctly included in project

### Issue: "init fail" error
**Solutions:**
1. Close any other apps using RFID hardware
2. Grant all necessary permissions
3. Check if UART port is available
4. Try calling `free()` then `init()` again

### Issue: Tags not reading
**Solutions:**
1. **Distance:** Bring tag within 0.5-2 meters
2. **Orientation:** Try different tag orientations
3. **Interference:** Move away from metal objects
4. **Power:** Check antenna power setting (demo app has this in settings)
5. **Frequency:** Ensure frequency region matches your country

### Issue: Write fails
**Solutions:**
1. **Tag Type:** Ensure tag is writable (not read-only)
2. **Password:** Some tags require correct access password
3. **Data Format:** Ensure data is valid hex string
4. **Length:** EPC data must be divisible by 4 (full words)

### Issue: "Flow abortion" errors
**Status:** ✅ Fixed in current implementation
- All blocking SDK calls now use `withContext(Dispatchers.IO)`

---

## Comparing with Demo App

### If our app doesn't work but demo works:

1. **Check Settings:**
   - Open demo app UHF settings tab
   - Note power, frequency, session settings
   - Match these in our app if needed

2. **Check SDK Version:**
   ```
   Demo AAR: DeviceAPI_ver20250209_release.aar
   Our AAR: [Check app/libs/]
   ```
   - Should be same or compatible version

3. **Check Initialization:**
   - Demo uses AsyncTask - we use coroutines
   - Both call same `init(context)` method
   - Should work identically

4. **Check Method Calls:**
   - Use this document to verify our calls match demo
   - All key methods should be identical

---

## Debugging Commands

### View Logcat for RFID Operations
```bash
adb logcat | grep "C72RfidReader"
```

### Check for Errors
```bash
adb logcat | grep -E "(RFID|UHF|Error|Exception)"
```

### Clear App Data and Reinstall
```bash
adb uninstall com.example.assettracking
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Success Criteria

### ✅ Minimum Viable RFID Functionality

- [ ] App initializes RFID without crashes
- [ ] Can read at least one RFID tag
- [ ] Can write asset ID to tag
- [ ] Read tag shows correct asset data
- [ ] No ANR (Application Not Responding) errors

### ✅ Full Functionality

- [ ] All above minimum criteria
- [ ] Bulk scan works with multiple tags
- [ ] Write operation with confirmation dialog
- [ ] Error messages are clear and helpful
- [ ] Performance is smooth (no UI freezing)

---

## Performance Benchmarks (from demo)

- **Single tag read:** < 100ms
- **Write operation:** < 500ms
- **Bulk inventory rate:** ~50-100 tags/second
- **Read range:** 0.5-5 meters (depending on tag and power)

---

## Next Steps After Testing

### If Everything Works ✅
1. Test with large dataset (100+ assets)
2. Test error scenarios (tag out of range, etc.)
3. Optimize UX (loading indicators, sounds)
4. Add analytics/logging for production

### If Issues Found ❌
1. Document exact error message
2. Check Logcat for stack trace
3. Compare behavior with demo app
4. Consult RFID_IMPLEMENTATION_ANALYSIS.md
5. Check Chainway SDK documentation

---

**Remember:** The demo app is your reference - if something doesn't work in our app but works in demo, check the settings and configuration in the demo app.

**Testing Date:** _________________  
**Device Serial:** _________________  
**Tester Name:** _________________  
**Results:** _________________
