<#ftl output_format="XHTML">
<#setting locale="en_US">
<#-- Model: report = com.parcels.report.dto.MonthlyReport, fontFamily = embedded font name.
     Rendered by Flying Saucer: must be well-formed XHTML; CSS 2.1 plus paged media. -->
<#function money v="">
    <#if (v!"")?is_number><#return v?string(",##0.00")></#if>
    <#return "–">
</#function>
<#function pct v="">
    <#if (v!"")?is_number><#return v?string("0.0") + "%"></#if>
    <#return "–">
</#function>
<#function bar v="">
    <#if (v!"")?is_number><#return [[v, 100]?min, 0]?max?c></#if>
    <#return "0">
</#function>
<html xmlns="http://www.w3.org/1999/xhtml" lang="en">
<head>
<meta charset="UTF-8"/>
<title>Finance report – ${report.monthLabel()}</title>
<style type="text/css">
    @page {
        size: A4;
        margin: 18mm 16mm 20mm 16mm;
        @bottom-left { content: "Finance report · ${report.monthLabel()}"; font-family: '${fontFamily}'; font-size: 8pt; color: #8a94a6; }
        @bottom-right { content: "Page " counter(page) " of " counter(pages); font-family: '${fontFamily}'; font-size: 8pt; color: #8a94a6; }
    }
    body { font-family: '${fontFamily}'; font-size: 9.5pt; color: #1f2937; margin: 0; }
    h1 { font-size: 24pt; margin: 2pt 0 4pt 0; color: #111827; }
    h2 { font-size: 12.5pt; margin: 18pt 0 6pt 0; color: #111827; padding-bottom: 3pt; border-bottom: 1.5pt solid #2563eb; }
    h3 { font-size: 10pt; margin: 10pt 0 4pt 0; color: #374151; }
    .eyebrow { font-size: 8pt; letter-spacing: 1.5pt; text-transform: uppercase; color: #2563eb; font-weight: bold; }
    .meta { color: #6b7280; font-size: 9pt; }
    .muted { color: #6b7280; }
    .note { color: #8a94a6; font-size: 8pt; margin-top: 4pt; }
    .pos { color: #047857; }
    .neg { color: #b91c1c; }
    table { width: 100%; border-collapse: collapse; }
    th { text-align: left; font-size: 7.5pt; text-transform: uppercase; letter-spacing: .6pt; color: #6b7280;
         padding: 4pt 6pt; border-bottom: 1pt solid #d1d5db; }
    td { padding: 4pt 6pt; border-bottom: 0.5pt solid #e5e7eb; vertical-align: middle; }
    .num { text-align: right; white-space: nowrap; }
    .nowrap { white-space: nowrap; }
    tr { page-break-inside: avoid; }
    .paginate { -fs-table-paginate: paginate; }
    .paginate tbody tr:nth-child(even) td { background-color: #f9fafb; }

    .kpis { margin-top: 6pt; }
    .kpis td { border: none; padding: 0 0 8pt 0; }
    .card { border: 0.75pt solid #e5e7eb; background-color: #f9fafb; padding: 8pt 10pt; }
    .card table td { border: none; padding: 0 6pt 0 0; vertical-align: top; }
    .ccy { font-size: 14pt; font-weight: bold; color: #111827; width: 15%; }
    .label { font-size: 7.5pt; text-transform: uppercase; letter-spacing: .6pt; color: #6b7280; }
    .big { font-size: 13pt; font-weight: bold; }
    .small { font-size: 8pt; }

    .track { background-color: #e5e7eb; height: 6pt; width: 100%; }
    .fill { height: 6pt; background-color: #2563eb; }
    .keep { page-break-inside: avoid; }
    .new-page { page-break-before: always; }
</style>
</head>
<body>

<div class="eyebrow">Monthly finance report</div>
<h1>${report.monthLabel()}</h1>
<div class="meta">
    <#if report.people()?has_content>${report.people()?join(" · ")} · </#if>${report.transactions()?size} transaction<#if report.transactions()?size != 1>s</#if>
    · generated ${report.generatedAt().toLocalDate().toString()} ${report.generatedAt().toLocalTime().withNano(0).withSecond(0).toString()}
</div>

<h2>Overview</h2>
<#if report.totals()?has_content>
<table class="kpis">
    <#list report.totals() as t>
    <tr><td>
        <div class="card">
            <table>
                <tr>
                    <td class="ccy">${t.currency().name()}</td>
                    <td style="width: 21%;"><div class="label">Income</div><div class="big pos">${money(t.income())}</div>
                        <div class="small muted">last month ${money(t.previousIncome())}</div></td>
                    <td style="width: 21%;"><div class="label">Expenses</div><div class="big neg">${money(t.expense())}</div>
                        <div class="small muted">last month ${money(t.previousExpense())}</div></td>
                    <td style="width: 21%;"><div class="label">Net</div>
                        <div class="big <#if t.net()?? && t.net() lt 0>neg<#else>pos</#if>">${money(t.net())}</div></td>
                    <td style="width: 22%;"><div class="label">vs last month</div>
                        <div class="big"><#if t.expenseChangePercent()??><#if t.expenseChangePercent() gt 0>▲ +<#elseif t.expenseChangePercent() lt 0>▼ </#if>${pct(t.expenseChangePercent())}<#else>–</#if></div></td>
                </tr>
            </table>
        </div>
    </td></tr>
    </#list>
</table>
<p class="note">Amounts in different currencies are never added together.</p>
<#if report.household()>
<div class="keep">
<h2>Per person</h2>
<table>
    <thead><tr><th style="width: 30%;">Person</th><th>Currency</th><th class="num">Income</th><th class="num">Expenses</th><th class="num">Net</th></tr></thead>
    <tbody>
    <#list report.personTotals() as p>
    <tr>
        <td><b>${p.person()}</b></td>
        <td class="muted">${p.currency().name()}</td>
        <td class="num pos">${money(p.income())}</td>
        <td class="num neg">${money(p.expense())}</td>
        <td class="num <#if p.net() lt 0>neg<#else>pos</#if>">${money(p.net())}</td>
    </tr>
    </#list>
    </tbody>
</table>
</div>
</#if>
<#else>
<p class="muted">No transactions were recorded for ${report.monthLabel()}.</p>
</#if>

<#if report.categories()?has_content>
<h2>Expenses by category</h2>
<#assign currencies = []>
<#list report.categories() as c><#if !currencies?seq_contains(c.currency().name())><#assign currencies = currencies + [c.currency().name()]></#if></#list>
<#list currencies as ccy>
<div class="keep">
<#if currencies?size gt 1><h3>${ccy}</h3></#if>
<table>
    <thead><tr><th style="width: 30%;">Category</th><th style="width: <#if report.household()>22<#else>38</#if>%;">Share of expenses</th><th class="num">Share</th>
        <#if report.household()><#list report.people() as person><th class="num">${person?keep_before(" ")}</th></#list></#if>
        <th class="num">Amount</th></tr></thead>
    <tbody>
    <#list report.categories()?filter(c -> c.currency().name() == ccy) as c>
    <tr>
        <td>${c.category()} <span class="muted small">· ${c.count()}</span></td>
        <td><div class="track"><div class="fill" style="width: ${bar(c.sharePercent())}%;"></div></div></td>
        <td class="num muted">${pct(c.sharePercent())}</td>
        <#if report.household()><#list report.people() as person><td class="num muted"><#if c.byPerson()[person]??>${money(c.byPerson()[person])}<#else>–</#if></td></#list></#if>
        <td class="num">${money(c.amount())} ${ccy}</td>
    </tr>
    </#list>
    </tbody>
</table>
</div>
</#list>
<#if report.multiCategoryTransactions()>
<p class="note">Some transactions have several categories and count in full under each one, so category amounts can add up to more than the total.</p>
</#if>
</#if>

<#if report.topMerchants()?has_content>
<div class="keep">
<h2>Top merchants</h2>
<table>
    <thead><tr><th>Merchant</th><th class="num">Visits</th><th class="num">Spent</th></tr></thead>
    <tbody>
    <#list report.topMerchants() as m>
    <tr><td>${m.merchant()}</td><td class="num">${m.count()}</td><td class="num">${money(m.amount())} ${m.currency().name()}</td></tr>
    </#list>
    </tbody>
</table>
</div>
</#if>

<#if report.transactions()?has_content>
<h2 class="new-page">Transactions</h2>
<table class="paginate">
    <thead><tr><th style="width: 13%;">Date</th><#if report.household()><th style="width: 12%;">Person</th></#if><th>Details</th><th style="width: 22%;">Category</th><th class="num" style="width: 17%;">Amount</th></tr></thead>
    <tbody>
    <#list report.transactions() as t>
    <#assign income = t.transactionType().name() == "INCOME">
    <tr>
        <td class="muted nowrap">${t.dateFrom().toLocalDate().toString()}</td>
        <#if report.household()><td>${(t.person()!"–")?keep_before(" ")}</td></#if>
        <td><#if t.merchant()??><b>${t.merchant()}</b><#if t.description()??> · </#if></#if>${t.description()!""}<#if t.items()?has_content> <span class="muted small">(${t.items()?size} item<#if t.items()?size != 1>s</#if>)</span></#if></td>
        <td class="muted">${t.categoryNames()?join(", ")}</td>
        <td class="num <#if income>pos<#else>neg</#if>"><#if income>+<#else>−</#if>${money(t.amount())} ${t.currency().name()}</td>
    </tr>
    </#list>
    </tbody>
</table>
</#if>

</body>
</html>
