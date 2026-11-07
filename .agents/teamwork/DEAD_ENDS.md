# Dead Ends Log

| Iteration | Approach Tried | Why It Failed | Files Touched |
|---|---|---|---|
| M1-It2 | Stripping table tags via regex `/<\/?(?:td\|tr\|table\|tbody\|thead)[^>]*>/i` returning `''` in `escapeHtml` | Lacked word boundaries and returned empty string; wiped out legitimate logistics and transport business remarks like `<truck-12>`, `<transport>`, `<transit>` | `app/www/index.html` |
| M1-It2 | Using raw `parseFloat(t.amount)` without stripping commas in voucher printing & reports | Truncates comma-formatted strings like `"1,00,000.00"` to `1`, causing receipt vouchers to print "One Rupees Only" | `app/www/index.html` |
