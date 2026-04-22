function doGet(e) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheets()[0]; // Or use ss.getSheetByName("Sheet1")

  var data = sheet.getRange(2, 1, 1, 2).getValues();
  var updateInfo = {
    versionCode: parseInt(data[0][0]),
    updateUrl: data[0][1]
  };

  return ContentService.createTextOutput(JSON.stringify(updateInfo))
    .setMimeType(ContentService.MimeType.JSON);
}