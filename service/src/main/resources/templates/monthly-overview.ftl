<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Spring Boot Email using FreeMarker</title>
</head>
<body style="font-family: 'Open Sans', sans-serif; line-height: 1.25%; margin: 0; padding: 0; background-color: #c5c3c3;">
<div style="margin-top: 10px; text-align: center;">Greetings, Marija</div>
<div style="text-align: center;">Have a nice day..!</div>

<table style="width: 100%; border-collapse: collapse; margin-top: 20px; border-radius: 8px; overflow: hidden; background-color: #ffffff; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);">
    <caption style="font-size: 1.5em; margin: 0.5em auto 0.75em; text-align: center;">Statement Summary</caption>
    <thead style="background-color: #f8f8f8;">
    <tr style="border: 1px solid #0a0909;">
        <th style="padding: 10px; text-align: center; font-size: 0.85em; letter-spacing: 0.1em; text-transform: uppercase;">
            Date
        </th>
        <th style="padding: 10px; text-align: center; font-size: 0.85em; letter-spacing: 0.1em; text-transform: uppercase;">
            Category
        </th>
        <th style="padding: 10px; text-align: center; font-size: 0.85em; letter-spacing: 0.1em; text-transform: uppercase;">
            Type
        </th>
        <th style="padding: 10px; text-align: center; font-size: 0.85em; letter-spacing: 0.1em; text-transform: uppercase;">
            Amount
        </th>
        <th style="padding: 10px; text-align: center; font-size: 0.85em; letter-spacing: 0.1em; text-transform: uppercase;">
            Currency
        </th>
    </tr>
    </thead>
    <tbody>
    <#list transactions as transaction>
        <tr style="background-color: #f8f8f8;">
            <td style="padding: 10px; text-align: center; font-size: 0.85em; border: 1px solid #0a0909;">${transaction.dateFrom}</td>
            <td style="padding: 10px; text-align: center; font-size: 0.85em; border: 1px solid #0a0909;">${transaction.category}
            </td>
            <td style="padding: 10px; text-align: center; font-size: 0.85em; border: 1px solid #0a0909;">${transaction.transactionType}</td>
            <td style="padding: 10px; text-align: center; font-size: 0.85em; border: 1px solid #0a0909;">${transaction.amount}
            </td>
            <td style="padding: 10px; text-align: center; font-size: 0.85em; border: 1px solid #0a0909;">${transaction.currency}
            </td>
        </tr>
    </#list>
    </tbody>
</table>

<p>Your balance for this month is ${balance} MKD.</p>
</body>
</html>
