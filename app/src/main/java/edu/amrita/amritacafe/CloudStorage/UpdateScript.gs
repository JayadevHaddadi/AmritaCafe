/**
 * Google Apps Script for Amrita Cafe Update System
 * Deploy this as a Web App:
 * - Execute as: Me
 * - Who has access: Anyone
 */
function doGet(e) {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const sheet = ss.getSheets()[0]; // Reads the first tab
  const data = sheet.getDataRange().getValues();

  let result = {
    versionCode: 0,
    updateUrl: "",
    betaVersionCode: 0,
    betaUpdateUrl: ""
  };

  // Start from index 1 to skip the header row
  for (let i = 1; i < data.length; i++) {
    const row = data[i];
    if (!row[0]) continue; // Skip empty rows

    const vCode = parseInt(row[0]);
    const url = row[1];
    const channel = row[2] ? row[2].toString().trim() : "Standard";

    if (channel === "Standard") {
      result.versionCode = vCode;
      result.updateUrl = url;
    } else if (channel === "Beta") {
      result.betaVersionCode = vCode;
      result.betaUpdateUrl = url;
    }
  }

  // Backwards compatibility: If no "Standard" was found but there is data,
  // use the first row as the default.
  if (result.versionCode === 0 && data.length > 1) {
    result.versionCode = parseInt(data[1][0]);
    result.updateUrl = data[1][1];
  }

  return ContentService.createTextOutput(JSON.stringify(result))
    .setMimeType(ContentService.MimeType.JSON);
}