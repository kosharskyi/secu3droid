## Function names use the app's IBM866 OEM convention

The desktop converts these names through the active Windows OEM code page; Android uses IBM866 to match its existing OEM text convention. Always consume the complete fixed 16-byte field, map each `0xFF` byte to a space before decoding, and stop the displayed name at the first NUL without trimming spaces. Count and index validation remains independent of name decoding.
