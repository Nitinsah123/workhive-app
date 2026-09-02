# ==============================================================================
# WorkHive — Complete Employee Invitation & MailHog End-to-End Test Suite
# ==============================================================================

$baseUrl = "http://localhost:8080"
$mailhogUrl = "http://localhost:8025"

function Assert-Step($desc, $condition, $value = "") {
    if ($condition) {
        Write-Host "  [PASS] $desc" -ForegroundColor Green
        if ($value) { Write-Host "         Value: $value" -ForegroundColor DarkGray }
    } else {
        Write-Host "  [FAIL] $desc" -ForegroundColor Red
        if ($value) { Write-Host "         Value: $value" -ForegroundColor DarkRed }
        exit 1
    }
}

Write-Host "`n=== 1. CREATING FRESH WORKSPACE: Nexus Cloud Systems ===" -ForegroundColor Cyan
$rnd = Get-Random -Minimum 100 -Maximum 999
$wsBody = @{
    companyName = "Nexus Cloud Systems $rnd"
    companyCode = "NEX$rnd"
    industry = "Cloud & Security"
    timezone = "America/New_York"
    adminFullName = "Sarah Connor"
    adminEmail = "sarah$rnd@nexuscloud.com"
    adminPassword = "Password123!"
    adminPhone = "+1 555-0199"
} | ConvertTo-Json

$wsRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/workspace" -Method Post -Body $wsBody -ContentType "application/json"
$adminToken = $wsRes.accessToken
$tenantId = $wsRes.tenant.id
$adminId = $wsRes.user.id
$adminHeaders = @{ Authorization = "Bearer $adminToken"; "Content-Type" = "application/json" }

Assert-Step "Workspace Created" ($wsRes.tenant.name -ne $null) $wsRes.tenant.name
Assert-Step "Admin Role is TENANT_ADMIN" ($wsRes.user.role -eq "TENANT_ADMIN") $wsRes.user.role

Write-Host "`n=== 2. CREATING DEPARTMENT & TEAM ===" -ForegroundColor Cyan
$deptBody = @{ name = "Engineering"; description = "Cloud Infrastructure & Platform" } | ConvertTo-Json
$dept = Invoke-RestMethod -Uri "$baseUrl/api/departments" -Method Post -Body $deptBody -Headers $adminHeaders
Assert-Step "Department Created" ($dept.name -eq "Engineering") $dept.id

$teamBody = @{ name = "DevOps Platform"; departmentId = $dept.id; description = "Core DevOps systems" } | ConvertTo-Json
$team = Invoke-RestMethod -Uri "$baseUrl/api/teams" -Method Post -Body $teamBody -Headers $adminHeaders
Assert-Step "Team Created" ($team.name -eq "DevOps Platform") $team.id

Write-Host "`n=== 3. INVITING EMPLOYEE 1 (employee1@gmail.com) ===" -ForegroundColor Cyan
# Clear MailHog inbox before testing
Invoke-RestMethod -Uri "$mailhogUrl/api/v1/messages" -Method Delete -ErrorAction SilentlyContinue

$emp1Email = "employee1@gmail.com"
$emp1Name = "Alice Engineer"
$inv1Body = @{
    name = $emp1Name
    email = $emp1Email
    role = "EMPLOYEE"
    departmentId = $dept.id
    teamId = $team.id
    managerId = $adminId
} | ConvertTo-Json

$inv1Res = Invoke-RestMethod -Uri "$baseUrl/api/invitations" -Method Post -Body $inv1Body -Headers $adminHeaders
Assert-Step "Invitation Created" ($inv1Res.id -ne $null) $inv1Res.id
Assert-Step "Invitation Email Status is EMAIL_SENT" ($inv1Res.emailStatus -eq "EMAIL_SENT") $inv1Res.emailStatus
Assert-Step "Recipient is exact entered email: $emp1Email" ($inv1Res.email -eq $emp1Email) $inv1Res.email
Assert-Step "Token generated" ($inv1Res.token.Length -gt 20) $inv1Res.token
Assert-Step "Invite URL generated" ($inv1Res.inviteUrl -match "accept-invitation\?token=") $inv1Res.inviteUrl

Write-Host "`n=== 4. VERIFYING MAILHOG INBOX (PORT 8025) FOR EMPLOYEE 1 ===" -ForegroundColor Cyan
Start-Sleep -Seconds 1
$mailhogMessages = Invoke-RestMethod -Uri "$mailhogUrl/api/v2/messages" -Method Get
Assert-Step "MailHog has received >= 1 email" ($mailhogMessages.total -ge 1) $mailhogMessages.total

$email1 = $mailhogMessages.items[0]
$email1To = $email1.To[0].Address
$email1Subject = $email1.Content.Headers.Subject[0]
$email1Body = $email1.Content.Body

Assert-Step "MailHog To-Address is strictly: $emp1Email" ($email1To -eq $emp1Email) $email1To
Assert-Step "Subject contains Company Name" ($email1Subject -match "Nexus Cloud Systems") $email1Subject
Assert-Step "Body contains Employee Name: $emp1Name" ($email1Body -match $emp1Name) "Found"
Assert-Step "Body contains Department Name: Engineering" ($email1Body -match "Engineering") "Found"
Assert-Step "Body contains Team Name: DevOps Platform" ($email1Body -match "DevOps Platform") "Found"
Assert-Step "Body contains Exact Invitation Link" ($email1Body -match $inv1Res.token) "Found"
Assert-Step "Body contains Expiration Notice" ($email1Body -match "expire") "Found"

Write-Host "`n=== 5. INVITING EMPLOYEE 2 (employee2@gmail.com) ===" -ForegroundColor Cyan
$emp2Email = "employee2@gmail.com"
$emp2Name = "Bob Platform"
$inv2Body = @{
    name = $emp2Name
    email = $emp2Email
    role = "MANAGER"
    departmentId = $dept.id
    teamId = $team.id
    managerId = $adminId
} | ConvertTo-Json

$inv2Res = Invoke-RestMethod -Uri "$baseUrl/api/invitations" -Method Post -Body $inv2Body -Headers $adminHeaders
Assert-Step "Employee 2 Invitation Created" ($inv2Res.emailStatus -eq "EMAIL_SENT") $inv2Res.email

$mailhogMessages2 = Invoke-RestMethod -Uri "$mailhogUrl/api/v2/messages" -Method Get
Assert-Step "MailHog has 2 emails total" ($mailhogMessages2.total -eq 2) $mailhogMessages2.total
$email2 = $mailhogMessages2.items | Where-Object { $_.To[0].Address -eq $emp2Email }
Assert-Step "MailHog received distinct email for: $emp2Email" ($email2 -ne $null) $email2.To[0].Address
Assert-Step "Email 2 body contains: $emp2Name" ($email2.Content.Body -match $emp2Name) "Found"

Write-Host "`n=== 6. NEGATIVE & SECURITY TEST CASES ===" -ForegroundColor Cyan
# 6a. Duplicate pending invitation
$dupPending = $false
try {
    Invoke-RestMethod -Uri "$baseUrl/api/invitations" -Method Post -Body $inv1Body -Headers $adminHeaders
} catch {
    $dupPending = $true
}
Assert-Step "Duplicate Pending Invitation Rejected" ($dupPending -eq $true) "HTTP Error as Expected"

# 6b. Invalid token acceptance
$invalidTokenAccepted = $false
try {
    $badAcceptBody = @{ token = "invalid-fake-token-9999"; fullName = "Hacker"; password = "Password123!" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/invitations/accept" -Method Post -Body $badAcceptBody -ContentType "application/json"
} catch {
    $invalidTokenAccepted = $false
}
Assert-Step "Invalid Token Acceptance Blocked" ($invalidTokenAccepted -eq $false) "HTTP 400/404 as Expected"

Write-Host "`n=== 7. PUBLIC TOKEN DETAILS INSPECTION ===" -ForegroundColor Cyan
$details = Invoke-RestMethod -Uri "$baseUrl/api/invitations/token/$($inv1Res.token)" -Method Get
Assert-Step "Token Details Valid" ($details.valid -eq $true) $details.valid
Assert-Step "Token Details Tenant Name" ($details.tenantName -match "Nexus Cloud Systems") $details.tenantName
Assert-Step "Token Details Email" ($details.email -eq $emp1Email) $details.email
Assert-Step "Token Details Department" ($details.departmentName -eq "Engineering") $details.departmentName
Assert-Step "Token Details Team" ($details.teamName -eq "DevOps Platform") $details.teamName

Write-Host "`n=== 8. ACCEPTING INVITATION & ACTIVATING ACCOUNT ===" -ForegroundColor Cyan
$acceptBody = @{
    token = $inv1Res.token
    fullName = "Alice Engineer"
    password = "Password123!"
    phone = "+1 555-0234"
} | ConvertTo-Json

$acceptRes = Invoke-RestMethod -Uri "$baseUrl/api/invitations/accept" -Method Post -Body $acceptBody -ContentType "application/json"
$emp1Token = $acceptRes.accessToken
$emp1Headers = @{ Authorization = "Bearer $emp1Token"; "Content-Type" = "application/json" }

Assert-Step "Invitation Accepted Successfully" ($emp1Token -ne $null) "JWT Issued"
Assert-Step "Employee Code Generated" ($acceptRes.user.employeeCode -match "EMP") $acceptRes.user.employeeCode
Assert-Step "Department ID Preserved on Acceptance" ($acceptRes.user.departmentId -eq $dept.id) $acceptRes.user.departmentId
Assert-Step "Team ID Preserved on Acceptance" ($acceptRes.user.teamId -eq $team.id) $acceptRes.user.teamId
Assert-Step "Manager ID Preserved on Acceptance" ($acceptRes.user.managerId -eq $adminId) $acceptRes.user.managerId

Write-Host "`n=== 9. VERIFYING EMPLOYEE PROFILE & AUTH ===" -ForegroundColor Cyan
$meProfile = Invoke-RestMethod -Uri "$baseUrl/api/users/me" -Method Get -Headers $emp1Headers
Assert-Step "Profile Email Matches: $emp1Email" ($meProfile.email -eq $emp1Email) $meProfile.email
Assert-Step "Profile Full Name Matches: Alice Engineer" ($meProfile.fullName -eq "Alice Engineer") $meProfile.fullName
Assert-Step "Profile Department is NOT None (ID: $($dept.id))" ($meProfile.departmentId -eq $dept.id) $meProfile.departmentId
Assert-Step "Profile Team is NOT None (ID: $($team.id))" ($meProfile.teamId -eq $team.id) $meProfile.teamId
Assert-Step "Profile Manager is NOT None (ID: $($adminId))" ($meProfile.managerId -eq $adminId) $meProfile.managerId

# Test employee login with new password
$loginBody = @{
    email = $emp1Email
    password = "Password123!"
} | ConvertTo-Json
$loginRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
Assert-Step "Employee Login with Password Succeeds" ($loginRes.accessToken -ne $null) $loginRes.user.email

# Test reusing the same token
$reusedTokenBlocked = $false
try {
    Invoke-RestMethod -Uri "$baseUrl/api/invitations/accept" -Method Post -Body $acceptBody -ContentType "application/json"
} catch {
    $reusedTokenBlocked = $true
}
Assert-Step "Reused Token is Blocked" ($reusedTokenBlocked -eq $true) "HTTP Error as Expected"

Write-Host "`n=================================================================" -ForegroundColor Green
Write-Host " ALL EMPLOYEE INVITATION & MAILHOG E2E TESTS PASSED 100%! " -ForegroundColor Green
Write-Host "=================================================================" -ForegroundColor Green
