## The Manager CSV ends with the RPM axis

Both exports enumerate wire rows in reverse load order and columns in increasing RPM order. CSV uses semicolons and has one decimal place in each correction cell, followed by one row of integer RPM labels without a final CRLF. It omits the load labels. Manager follows its interface locale when parsing decimals, so choose the matching decimal separator for the receiving Manager. Clipboard TSV contains only the selected cells and ends every row with CRLF.
