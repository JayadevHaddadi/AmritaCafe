/**
 * Handles GET requests.
 * - If 'sheetName' parameter is provided, returns the content of that sheet as CSV.
 * - Otherwise, returns a list of all sheet names.
 *
 * @param {Object} e The event parameter containing request parameters.
 * @return {ContentService.TextOutput} JSON response.
 */
function doGet(e) {
  try {
    // --- Option 2: If the script is STANDALONE (Recommended for clarity) ---
    const SPREADSHEET_ID = "1kUfAPm06nU4Trtm9zKJkX8hyk8biZpB6mOkVHKEn1Cg"; // <-- ****** Make sure this is your Sheet ID ******
    const ss = SpreadsheetApp.openById(SPREADSHEET_ID);

    // Check if a specific sheet name was requested
    const requestedSheetName = e.parameter.sheetName;

    if (requestedSheetName) {
      // --- Fetch content of the specified sheet ---
      Logger.log("Fetching content for sheet: " + requestedSheetName);
      const sheet = ss.getSheetByName(requestedSheetName);

      if (sheet) {
        const dataRange = sheet.getDataRange();
        const values = dataRange.getValues(); // Gets data as a 2D array

        // Convert the 2D array to a CSV string
        const csvContent = values.map(row => {
          return row.map(cell => {
            let cellValue = cell === null ? "" : String(cell);
            // Basic CSV quoting: quote if it contains comma, double quote, or newline
            if (cellValue.includes(',') || cellValue.includes('"') || cellValue.includes('\n')) {
              // Double up existing double quotes
              cellValue = cellValue.replace(/"/g, '""');
              // Enclose in double quotes
              cellValue = '"' + cellValue + '"';
            }
            return cellValue;
          }).join(','); // Join cells in a row with a comma
        }).join('\n'); // Join rows with a newline

        const result = {
          status: "success",
          sheetName: requestedSheetName,
          data: csvContent
        };
        return ContentService.createTextOutput(JSON.stringify(result))
          .setMimeType(ContentService.MimeType.JSON);

      } else {
        // Sheet not found
        Logger.log("Sheet not found: " + requestedSheetName);
        const errorResult = {
          status: "error",
          message: "Sheet '" + requestedSheetName + "' not found in the spreadsheet."
        };
        // Return a 404 status code as well
        return ContentService.createTextOutput(JSON.stringify(errorResult))
          .setMimeType(ContentService.MimeType.JSON)
          // Optional: Set HTTP status code for not found
          // .setStatusCode(404); // Note: Client might just see the JSON error
      }

    } else {
      // --- List all sheet names (original functionality) ---
      Logger.log("Listing all sheet names.");
      const sheets = ss.getSheets();
      const sheetNames = sheets.map(sheet => sheet.getName());
      const result = {
        status: "success",
        sheetNames: sheetNames
      };
      return ContentService.createTextOutput(JSON.stringify(result))
        .setMimeType(ContentService.MimeType.JSON);
    }

  } catch (error) {
    // Log the error for debugging
    Logger.log("Error in doGet: " + error);
    // Prepare an error JSON response
    const errorResult = {
      status: "error",
      message: "Script Error: " + error.message
    };
    return ContentService.createTextOutput(JSON.stringify(errorResult))
      .setMimeType(ContentService.MimeType.JSON);
      // Optional: Set HTTP status code for internal server error
      // .setStatusCode(500); // Note: Client might just see the JSON error
  }
}

// --- Optional: Helper function to test directly in the script editor ---
function testGetSheetContent() {
  // Simulate the event object 'e' for testing
  const mockEvent = {
    parameter: {
      // Replace 'Sheet1' with a valid sheet name from YOUR spreadsheet
      sheetName: "Sheet1"
    }
  };
  const jsonOutput = doGet(mockEvent);
  Logger.log("Test GetSheetContent Output:\n" + jsonOutput.getContent());
}

function testGetAllSheetNames() {
  // Simulate event object without sheetName parameter
   const mockEvent = {
    parameter: {}
  };
  const jsonOutput = doGet(mockEvent);
  Logger.log("Test GetAllSheetNames Output:\n" + jsonOutput.getContent());
}