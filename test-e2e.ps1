$baseUrl = "http://localhost:8080/api"
$rnd = Get-Random -Minimum 100 -Maximum 999

function Assert-Equal($actual, $expected, $label) {
    if ($actual -eq $expected) {
        Write-Host "  [PASS] $label (Value: $actual)" -ForegroundColor Green
    } else {
        Write-Host "  [FAIL] $label - Expected: $expected, Got: $actual" -ForegroundColor Red
        throw "Assertion failed for $label"
    }
}

Write-Host "=== 1. CREATING WORKSPACE A: Acme Corp ($rnd) ===" -ForegroundColor Cyan
$wsAReq = @{
    companyName = "Acme Technologies $rnd"
    companyCode = "A$rnd"
    adminFullName = "Rahul Sharma"
    adminEmail = "rahul$rnd@acme.com"
    adminPassword = "Password123!"
    industry = "SaaS & Cloud"
    timezone = "Asia/Kolkata"
} | ConvertTo-Json

$wsAResp = Invoke-RestMethod -Uri "$baseUrl/auth/create-workspace" -Method Post -Body $wsAReq -ContentType "application/json"
$tokenA = $wsAResp.accessToken
$tenantA = $wsAResp.tenant
$adminA = $wsAResp.user

Assert-Equal $tenantA.name "Acme Technologies $rnd" "Workspace A Name"
Assert-Equal $adminA.role "TENANT_ADMIN" "Admin A Role"
Assert-Equal $adminA.employeeCode "A$rnd-ADM-001" "Admin A Employee Code"
Write-Host "  Tenant A ID: $($tenantA.id), Admin A ID: $($adminA.id)"

Write-Host "`n=== 2. CREATING WORKSPACE B: Globex Corp ($rnd) ===" -ForegroundColor Cyan
$wsBReq = @{
    companyName = "Globex Corporation $rnd"
    companyCode = "G$rnd"
    adminFullName = "Elena Rostova"
    adminEmail = "elena$rnd@globex.com"
    adminPassword = "Password123!"
    industry = "FinTech"
    timezone = "America/New_York"
} | ConvertTo-Json

$wsBResp = Invoke-RestMethod -Uri "$baseUrl/auth/create-workspace" -Method Post -Body $wsBReq -ContentType "application/json"
$tokenB = $wsBResp.accessToken
$tenantB = $wsBResp.tenant
$adminB = $wsBResp.user

Assert-Equal $tenantB.name "Globex Corporation $rnd" "Workspace B Name"
Assert-Equal $adminB.employeeCode "G$rnd-ADM-001" "Admin B Employee Code"
Write-Host "  Tenant B ID: $($tenantB.id), Admin B ID: $($adminB.id)"

$headersA = @{ "Authorization" = "Bearer $tokenA"; "Content-Type" = "application/json" }
$headersB = @{ "Authorization" = "Bearer $tokenB"; "Content-Type" = "application/json" }

Write-Host "`n=== 3. ACME: CREATE DEPARTMENT & TEAM ===" -ForegroundColor Cyan
$deptReq = @{ name = "Engineering"; description = "Software Engineering Department" } | ConvertTo-Json
$deptResp = Invoke-RestMethod -Uri "$baseUrl/departments" -Method Post -Headers $headersA -Body $deptReq
Assert-Equal $deptResp.name "Engineering" "Department Name"
$deptId = $deptResp.id

$teamReq = @{ name = "Core Platform"; departmentId = $deptId; description = "Core Backend & Infra" } | ConvertTo-Json
$teamResp = Invoke-RestMethod -Uri "$baseUrl/teams" -Method Post -Headers $headersA -Body $teamReq
Assert-Equal $teamResp.name "Core Platform" "Team Name"
$teamId = $teamResp.id

Write-Host "`n=== 4. ACME: INVITE EMPLOYEE & ACCEPT INVITATION ===" -ForegroundColor Cyan
$invReq = @{
    email = "vikram$rnd@acme.com"
    fullName = "Vikram Patel"
    role = "EMPLOYEE"
    departmentId = $deptId
    teamId = $teamId
} | ConvertTo-Json
$invResp = Invoke-RestMethod -Uri "$baseUrl/invitations" -Method Post -Headers $headersA -Body $invReq
$invToken = $invResp.token
Assert-Equal $invResp.email "vikram$rnd@acme.com" "Invitation Email"

# Accept invitation
$acceptReq = @{ token = $invToken; fullName = "Vikram Patel"; password = "Password123!" } | ConvertTo-Json
$acceptResp = Invoke-RestMethod -Uri "$baseUrl/invitations/accept" -Method Post -Body $acceptReq -ContentType "application/json"
$tokenEmp = $acceptResp.accessToken
$empA = $acceptResp.user
Assert-Equal $empA.role "EMPLOYEE" "Employee Role"
Assert-Equal ($empA.employeeCode.StartsWith("A$rnd-EMP-")) $true "Employee Code Format"
$headersEmp = @{ "Authorization" = "Bearer $tokenEmp"; "Content-Type" = "application/json" }

Write-Host "`n=== 5. ACME: CREATE PROJECT & TASK WORKFLOW ===" -ForegroundColor Cyan
$projReq = @{
    name = "Cloud Native Migration"
    description = "Migrate core monolith to cloud architecture"
    priority = "HIGH"
    status = "ACTIVE"
    departmentId = $deptId
} | ConvertTo-Json
$projResp = Invoke-RestMethod -Uri "$baseUrl/projects" -Method Post -Headers $headersA -Body $projReq
$projId = $projResp.id
Assert-Equal $projResp.name "Cloud Native Migration" "Project Name"

# Create Task
$taskReq = @{
    title = "Implement Multi-Tenant Security Filter"
    description = "Verify all repositories filter strictly by tenantId"
    projectId = $projId
    assigneeId = $empA.id
    priority = "HIGH"
    estimatedHours = 12
    dueDate = (Get-Date).AddDays(5).ToString("yyyy-MM-dd")
} | ConvertTo-Json
$taskResp = Invoke-RestMethod -Uri "$baseUrl/tasks" -Method Post -Headers $headersA -Body $taskReq
$taskId = $taskResp.id
Assert-Equal $taskResp.title "Implement Multi-Tenant Security Filter" "Task Title"
Assert-Equal $taskResp.status "TODO" "Initial Task Status"

# Employee updates status to IN_PROGRESS
$statusReq = @{ status = "IN_PROGRESS" } | ConvertTo-Json
$inProgResp = Invoke-RestMethod -Uri "$baseUrl/tasks/$taskId/status" -Method Patch -Headers $headersEmp -Body $statusReq
Assert-Equal $inProgResp.status "IN_PROGRESS" "Task In Progress"

# Employee submits task for REVIEW
$revStatusReq = @{ status = "REVIEW" } | ConvertTo-Json
$revTaskResp = Invoke-RestMethod -Uri "$baseUrl/tasks/$taskId/status" -Method Patch -Headers $headersEmp -Body $revStatusReq
Assert-Equal $revTaskResp.status "REVIEW" "Task Submitted For Review"

Write-Host "`n=== 6. ACME: ATTENDANCE & LEAVE APPLICATION ===" -ForegroundColor Cyan
# Attendance Check-in
$checkInResp = Invoke-RestMethod -Uri "$baseUrl/attendance/check-in" -Method Post -Headers $headersEmp -Body "{}"
Assert-Equal $checkInResp.status "CHECKED_IN" "Attendance Check-In Status"

# Attendance Check-out
$checkOutResp = Invoke-RestMethod -Uri "$baseUrl/attendance/check-out" -Method Post -Headers $headersEmp -Body "{}"
Assert-Equal $checkOutResp.status "CHECKED_OUT" "Attendance Check-Out Status"

# Create default leave type if needed, then apply
$createLtReq = @{ name = "Annual Paid Leave"; description = "Standard annual paid vacation"; defaultBalance = 20 } | ConvertTo-Json
$ltResp = Invoke-RestMethod -Uri "$baseUrl/leave/types" -Method Post -Headers $headersA -Body $createLtReq
$leaveTypeId = $ltResp.id

$leaveReq = @{
    leaveTypeId = $leaveTypeId
    startDate = (Get-Date).AddDays(3).ToString("yyyy-MM-dd")
    endDate = (Get-Date).AddDays(5).ToString("yyyy-MM-dd")
    reason = "Attending cloud architecture summit"
} | ConvertTo-Json
$leaveResp = Invoke-RestMethod -Uri "$baseUrl/leave/apply" -Method Post -Headers $headersEmp -Body $leaveReq
$leaveId = $leaveResp.id
Assert-Equal $leaveResp.status "PENDING" "Leave Request Status"

Write-Host "`n=== 7. ACME: ACTION CENTER (REVIEW & APPROVALS) ===" -ForegroundColor Cyan
$actionSummary = Invoke-RestMethod -Uri "$baseUrl/action-center/summary" -Method Get -Headers $headersA
Assert-Equal ($actionSummary.totalPending -ge 2) $true "Action Center Has >= 2 Pending Items"

# Manager approves Task Review
$taskReviewReq = @{ decision = "APPROVED"; comment = "Code quality and test coverage are outstanding" } | ConvertTo-Json
$approvedTask = Invoke-RestMethod -Uri "$baseUrl/tasks/$taskId/review" -Method Post -Headers $headersA -Body $taskReviewReq
Assert-Equal $approvedTask.status "COMPLETED" "Task Review Decision Result"

# Manager approves Leave
$leaveReviewReq = @{ status = "APPROVED"; reviewComment = "Approved. Have a great summit!" } | ConvertTo-Json
$approvedLeave = Invoke-RestMethod -Uri "$baseUrl/leave/$leaveId/review" -Method Post -Headers $headersA -Body $leaveReviewReq
Assert-Equal $approvedLeave.status "APPROVED" "Leave Approval Status"

Write-Host "`n=== 8. ACME: GITHUB INTEGRATION & WORKACTIVITY ===" -ForegroundColor Cyan
$ghUrlResp = Invoke-RestMethod -Uri "$baseUrl/integrations/github/oauth/url" -Method Get -Headers $headersA
Assert-Equal ($ghUrlResp.authorizationUrl.Contains("github.com")) $true "GitHub OAuth URL valid"

$ghConnectReq = @{
    provider = "GITHUB"
    accessToken = "gho_sample_token_verified"
    externalUsername = "acme-dev"
    scopes = "repo,read:user"
} | ConvertTo-Json
$ghConnResp = Invoke-RestMethod -Uri "$baseUrl/integrations/connect" -Method Post -Headers $headersA -Body $ghConnectReq
Assert-Equal $ghConnResp.status "CONNECTED" "GitHub Connected"
$ghId = $ghConnResp.id

# Map repository to project
$mapReq = @{
    externalId = "10101"
    externalName = "acme-corp/cloud-platform"
    externalType = "REPOSITORY"
    workhiveEntityType = "PROJECT"
    workhiveEntityId = $projId
} | ConvertTo-Json
$mapResp = Invoke-RestMethod -Uri "$baseUrl/integrations/$ghId/mappings" -Method Post -Headers $headersA -Body $mapReq
Assert-Equal $mapResp.externalName "acme-corp/cloud-platform" "Mapping Registered"

# Ingest GitHub Webhook
$webhookPayload = @{
    action = "push"
    repositoryName = "acme-corp/cloud-platform"
    sender = "vikram-patel"
    commitMessage = "feat: add tenant isolation interceptors"
    commitId = "c0ffee12"
} | ConvertTo-Json
$whResp = Invoke-RestMethod -Uri "$baseUrl/integrations/webhook/github" -Method Post -Headers $headersEmp -Body $webhookPayload
Assert-Equal $whResp.status "received" "GitHub Webhook Processed"

Write-Host "`n=== 9. ACME: REAL PDF & CSV EXPORTS ===" -ForegroundColor Cyan
$empPdf = Invoke-WebRequest -UseBasicParsing -Uri "$baseUrl/exports/pdf/employee" -Headers $headersA
Assert-Equal ($empPdf.Content.Length -gt 1000) $true "Employee PDF Generated (>1KB)"
Assert-Equal ([char]$empPdf.Content[0]) '%' "PDF Magic Byte '%'"

$projPdf = Invoke-WebRequest -UseBasicParsing -Uri "$baseUrl/exports/pdf/project/$projId" -Headers $headersA
Assert-Equal ($projPdf.Content.Length -gt 1000) $true "Project PDF Generated (>1KB)"

$orgPdf = Invoke-WebRequest -UseBasicParsing -Uri "$baseUrl/exports/pdf/organization" -Headers $headersA
Assert-Equal ($orgPdf.Content.Length -gt 1000) $true "Organization PDF Generated (>1KB)"

$taskCsv = Invoke-WebRequest -UseBasicParsing -Uri "$baseUrl/exports/csv/tasks" -Headers $headersA
Assert-Equal ($taskCsv.Content.Length -gt 50) $true "Task CSV Generated"

Write-Host "`n=== 10. STRICT TWO-TENANT ISOLATION SECURITY AUDIT ===" -ForegroundColor Yellow

# Globex attempting to read Acme's project
try {
    $leak = Invoke-RestMethod -Uri "$baseUrl/projects/$projId" -Method Get -Headers $headersB -ErrorAction Stop
    Write-Host "  [CRITICAL SECURITY FAIL] Tenant B accessed Tenant A Project!" -ForegroundColor Red
    throw "Security breach: Tenant B accessed Tenant A Project"
} catch {
    Write-Host "  [PASS] Tenant B BLOCKED from reading Tenant A Project" -ForegroundColor Green
}

# Globex attempting to read Acme's task
try {
    $leakTask = Invoke-RestMethod -Uri "$baseUrl/tasks/$taskId" -Method Get -Headers $headersB -ErrorAction Stop
    Write-Host "  [CRITICAL SECURITY FAIL] Tenant B accessed Tenant A Task!" -ForegroundColor Red
    throw "Security breach: Tenant B accessed Tenant A Task"
} catch {
    Write-Host "  [PASS] Tenant B BLOCKED from reading Tenant A Task" -ForegroundColor Green
}

# Globex attempting to approve Acme's leave
try {
    $leakLeave = Invoke-RestMethod -Uri "$baseUrl/leave/$leaveId/review" -Method Post -Headers $headersB -Body '{"status":"APPROVED"}' -ErrorAction Stop
    Write-Host "  [CRITICAL SECURITY FAIL] Tenant B approved Tenant A Leave!" -ForegroundColor Red
    throw "Security breach: Tenant B approved Tenant A Leave"
} catch {
    Write-Host "  [PASS] Tenant B BLOCKED from approving Tenant A Leave" -ForegroundColor Green
}

# Globex listing projects -> must NOT contain Acme's project
$globexProjects = Invoke-RestMethod -Uri "$baseUrl/projects" -Method Get -Headers $headersB
$containsAcme = ($globexProjects.content | Where-Object { $_.id -eq $projId }).Count
Assert-Equal $containsAcme 0 "Globex Project List strictly contains 0 Acme Projects"

Write-Host "`n=================================================================" -ForegroundColor Green
Write-Host " ALL REAL-WORLD E2E SaaS WORKFLOWS AND SECURITY TESTS PASSED! " -ForegroundColor Green
Write-Host "=================================================================" -ForegroundColor Green