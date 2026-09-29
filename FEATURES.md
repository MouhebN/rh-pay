# RH Paie - Implemented Features

This document summarizes the features already implemented in the HR/payroll management application.

## Project Structure

- Spring Boot backend in `backend/`
- Angular frontend in `frontend/`
- PostgreSQL database with Flyway migrations
- Feature-based Angular architecture
- Modular backend package architecture

## Authentication and Security

- JWT authentication
- Register
- Login
- Refresh token
- Logout
- Current user endpoint
- Role-based access control
- Roles:
  - `ADMIN_RH`
  - `EMPLOYE`
- Auth guard in Angular
- Role guard in Angular
- Auth interceptor in Angular
- Token storage service
- Login audit
- Refresh token persistence
- Account lock/unlock
- Account enable/disable

## User Management

- Admin user listing
- Create user account
- Edit user account
- Enable/disable user
- Unlock locked account
- Delete user
- Create employee with account options:
  - no account
  - link existing account
  - create account during employee creation
- Create employee with optional first contract in the same onboarding form
- Employee onboarding supports:
  - employee administrative data
  - optional user account
  - optional first contract

## Organization Management

- Department CRUD
- Job position CRUD
- Job positions can be linked to departments

## Employee Management

- Employee CRUD
- Employee status update
- Employee can be linked to a user account
- Employee fields include:
  - matricule
  - first name
  - last name
  - email
  - phone
  - birth date
  - hire date
  - department
  - job position
  - base salary
  - user account
  - status

## Contract Management / Gestion des contrats

- Admin-only contract management for employee contracts
- Employee access to own contracts only
- First contract can be created during employee onboarding without removing the standalone contract module
- Routes:
  - `/admin/contracts`
  - `/admin/contracts/monitoring`
  - `/admin/contracts/create`
  - `/admin/contracts/:id`
  - `/admin/contracts/:id/edit`
  - `/employee/my-contracts`
- Contract types:
  - `CDI`
  - `CDD`
  - `STAGE`
  - `FREELANCE`
  - `APPRENTICESHIP`
  - `OTHER`
- Contract statuses:
  - `DRAFT`
  - `ACTIVE`
  - `EXPIRED`
  - `TERMINATED`
  - `RENEWED`
- Admin actions:
  - create contract
  - edit draft or active contract
  - activate draft contract
  - terminate contract with reason
  - renew active or expired contract
  - delete draft contract only
  - refresh expired contracts
  - upload signed PDF contract document
  - replace signed PDF contract document
  - download contract document
  - delete contract document
- Contract document rules:
  - one optional PDF document per contract
  - maximum file size: 5MB
  - stored locally under `uploads/contracts`
  - storage path is never exposed to the frontend
  - employees can download only their own contract document
- Business rules:
  - one active contract per employee
  - CDI can have no end date
  - fixed-term contract types require an end date
  - contract dates and trial period dates are validated
  - salary at signature must be positive
  - contract reference is generated automatically when omitted
- Admin dashboard contract cards:
  - active contracts
  - contracts expiring soon
  - expired contracts
- Employee dashboard current contract card

## Contract Monitoring / Suivi des contrats

- Admin-only contract monitoring page
- Route:
  - `/admin/contracts/monitoring`
- Monitoring endpoints:
  - `/api/contracts/monitoring`
  - `/api/contracts/expiring-soon`
  - `/api/contracts/expired`
  - `/api/contracts/missing-documents`
  - `/api/contracts/refresh-expired`
- Summary cards:
  - active contracts
  - contracts expiring soon
  - expired contracts
  - missing signed documents
  - HR actions required
- HR actions:
  - view contract
  - renew contract
  - prepare employee exit
  - add signed PDF
  - refresh expired contracts
- Important rule:
  - contract expiration only marks the contract as `EXPIRED`
  - it does not disable the employee account
  - it does not mark the employee as `LEFT`
  - HR chooses renewal, exit preparation, or temporary active continuation

## Employee Exit / Départ employé

- Admin-only employee offboarding module
- Route:
  - `/admin/employee-exits`
  - `/admin/employee-exits/create`
  - `/admin/employee-exits/:id`
- Exit types:
  - `RESIGNATION`
  - `END_OF_CONTRACT`
  - `DISMISSAL`
  - `RETIREMENT`
  - `MUTUAL_AGREEMENT`
  - `OTHER`
- Exit statuses:
  - `DRAFT`
  - `SCHEDULED`
  - `COMPLETED`
  - `CANCELLED`
- Admin actions:
  - prepare employee exit
  - schedule employee exit
  - complete employee exit
  - cancel employee exit with reason
  - process due scheduled exits
  - attach exit documents
  - download exit documents
  - delete exit documents
- Exit document types:
  - resignation letter
  - termination letter
  - end-of-contract document
  - final settlement
  - work certificate
  - other HR document
- Exit document rules:
  - multiple documents per exit
  - accepted formats: PDF, PNG, JPG/JPEG
  - maximum size: 5MB
  - stored locally under `uploads/employee-exits`
  - storage path is never exposed
  - visible documents can be downloaded by the employee
- Completion effects:
  - employee status becomes `LEFT`
  - linked user account is disabled
  - active contract is terminated through the contract service
  - historical employee data is preserved
- Attendance integration:
  - exited or inactive employees cannot check in or check out
  - daily absence generation remains scoped to active employees
- Payroll variables integration:
  - employees who left before the selected payroll month are excluded
  - employees who left during the selected month remain included for manual adjustment
- Admin dashboard exit cards:
  - scheduled exits
  - upcoming exits
  - completed exits

## Leave Requests / Demandes d'absence

- Employee can create leave request for himself
- Admin can create leave request for any employee
- Admin-created leave requests are approved automatically
- Employee-created leave requests start as pending
- Admin can approve pending requests
- Admin can reject pending requests with rejection reason
- Employee can cancel own pending request
- Admin can cancel pending requests
- Employee sees only own leave requests
- Admin sees all leave requests
- Filters for admin leave requests:
  - status
  - type
  - employee
  - department
  - start date
  - end date
- Leave request statuses:
  - `PENDING`
  - `APPROVED`
  - `REJECTED`
  - `CANCELLED`
- Leave types:
  - `PAID_LEAVE`
  - `SICK_LEAVE`
  - `UNPAID_LEAVE`
  - `MATERNITY_LEAVE`
  - `OTHER`
- Automatic total days calculation
- Overlap prevention for pending/approved leave requests

## Leave Balance / Solde de congé

- Yearly leave balance per employee
- Unique balance per employee and year
- Default acquired days: 24
- Tracks:
  - acquired days
  - consumed days
  - pending days
  - remaining days
- Paid leave request creation reserves pending days
- Paid leave approval consumes days
- Rejection releases pending days
- Cancellation releases pending days
- Admin-created paid leave consumes balance directly
- Insufficient balance blocks paid leave approval/creation
- Admin leave balance page
- Employee leave balance cards
- Edit acquired days from admin UI
- Remaining balance visual badges:
  - green
  - orange
  - red

## Advanced Leave Approval Validation

- Validation endpoint before approval
- Admin can verify a leave request before approval
- Blocking errors prevent approval
- Warnings allow approval after confirmation
- Validation checks:
  - request must be pending
  - start date before or equal end date
  - no overlapping pending/approved requests
  - paid leave has sufficient balance
  - employee must be active
  - unpaid leave affects payroll warning
  - more than 5 absence days in current month warning
  - close approved absence warning
  - weekend days warning
  - leave duration greater than 10 days warning
  - sick leave document warning

## Leave Statistics and Dashboard Analytics

- Admin absence statistics endpoint
- Employee personal absence statistics endpoint
- Admin dashboard absence cards:
  - demandes en attente
  - absences approuvées ce mois
  - jours d'absence ce mois
  - employés absents aujourd'hui
- Admin leave requests statistics section:
  - absences par mois
  - absences par type
  - absences par département
- Employee dashboard absence cards:
  - mes demandes en attente
  - mes absences approuvées cette année
  - mes jours d'absence cette année
  - mes congés sans solde
- Employee dashboard statistics sections:
  - absences par mois
  - absences par type
- Statistics calculated by the backend:
  - pending requests
  - approved requests this month/year
  - rejected requests this month/year
  - cancelled requests this month
  - approved leave days
  - unpaid leave days
  - employees currently on leave
  - monthly totals
  - totals by leave type
  - totals by department

## Absence Calendar

- Admin absence calendar endpoint
- Employee personal absence calendar endpoint
- Admin monthly calendar route:
  - `/admin/leave-calendar`
- Employee monthly calendar route:
  - `/employee/leave-calendar`
- Admin calendar filters:
  - department
  - status
  - leave type
- Employee calendar shows only personal leave requests
- Calendar displays pending and approved absences by default
- Event colors by status:
  - pending
  - approved
  - rejected
  - cancelled
- Event details modal with employee, period, duration, department, and job position
- Admin calendar links each event to the leave request details page

## Leave Request Supporting Documents

- Local document storage for leave requests
- Storage directory:
  - `uploads/leave-documents`
- Configurable upload directory with:
  - `FILE_UPLOAD_DIR`
- Leave request document metadata:
  - file name
  - content type
  - file size
  - uploaded date
- Upload document endpoint
- Download/view document endpoint
- Delete document endpoint
- Allowed document formats:
  - PDF
  - PNG
  - JPG/JPEG
- Maximum file size:
  - 5MB
- Employee can upload/delete only for own pending request
- Admin can upload/delete for any pending request
- Documents cannot be uploaded after approval, rejection, or cancellation
- Sick leave validation warns when no supporting document is provided
- Employee can select a supporting document during leave request creation
- Employee UI shows document status and pending-request document actions
- Admin leave details page shows document information and actions

## Notifications

- Persisted notification history
- Recent notifications endpoint:
  - `/api/notifications/recent`
- Native WebSocket notification endpoint:
  - `/ws/notifications`
- Top-bar notification dropdown
- Shows the latest notifications in the menu
- Toast message when a live notification arrives
- Admin notifications:
  - new employee leave request
  - employee cancelled leave request
- Employee notifications:
  - request approved
  - request rejected
  - request cancelled by RH
  - absence recorded by RH
- Admin sidebar shortcut for pending leave requests:
  - `Demandes en attente`

## Attendance / Pointage

- Manual and web attendance module
- Attendance sources:
  - `WEB`
  - `MANUAL`
  - `FACE_RECOGNITION` reserved for future use
- Attendance statuses:
  - `PRESENT`
  - `LATE`
  - `ABSENT`
  - `HALF_DAY`
  - `ON_LEAVE`
  - `INCOMPLETE`
- Employee attendance actions:
  - check in
  - check out
  - view today's attendance state
  - view personal attendance history
- Employee dashboard pointage card:
  - today's date
  - current status
  - arrival time
  - exit time
  - worked hours
  - check-in/check-out buttons
- Admin attendance page:
  - today's summary cards
  - attendance table
  - filters by employee, department, job position, date range, status, and source
  - manual adjustment modal
  - daily absence generation
- Automatic calculations:
  - worked hours
  - late minutes
  - overtime minutes
  - incomplete records
  - half-day records
- Integration with leave requests:
  - approved leave blocks employee check-in
  - daily generation creates `ON_LEAVE` records when an employee has approved leave
  - daily generation creates `ABSENT` records when no attendance or leave exists
- Notifications:
  - admins are notified when an employee checks in late
  - employees are notified when HR adjusts an attendance record

## Payroll Variables / Variables de paie

- Admin-only monthly payroll variable preparation
- Route:
  - `/admin/payroll-variables`
- Generate variables for active employees by month/year
- One variable record per employee per month/year
- Status workflow:
  - `DRAFT`
  - `VALIDATED`
  - `LOCKED` reserved for future payroll calculation
- Automatic values generated from existing modules:
  - unpaid absence days from approved unpaid leave
  - paid leave days from approved paid leave
  - sick leave days from approved sick leave
  - late minutes from attendance records
  - overtime minutes from attendance records
  - worked days from present/late attendance records
  - absent days from absent attendance records
- Manual HR values:
  - bonus amount
  - advance amount
  - deduction amount
  - notes
- Business rules:
  - draft variables can be edited
  - validated variables cannot be edited
  - validated variables can be reopened
  - locked variables cannot be reopened
  - draft variables can be deleted
  - negative manual amounts are rejected
- Admin UI:
  - month/year selector
  - generate button
  - summary cards
  - filters by employee, department, job position, status, month, and year
  - edit manual variables
  - validate and reopen actions
  - details modal
- Admin dashboard payroll cards:
  - variables brouillon ce mois
  - variables validées ce mois
  - total primes ce mois
  - total retenues ce mois

## Payroll Calculation / Calcul de paie

- Admin-only simplified monthly payroll calculation
- Employee access to finalized salary results only
- Routes:
  - `/admin/payroll-calculations`
  - `/employee/my-salaries`
- Calculation requires validated payroll variables
- Payroll variables become `LOCKED` after calculation
- Calculation statuses:
  - `DRAFT`
  - `FINALIZED`
  - `CANCELLED`
- Formula version 1:
  - daily rate = employee base salary / 30
  - hourly rate = employee base salary / 173.33
  - overtime amount = overtime hours * hourly rate * 1.25
  - unpaid absence deduction = unpaid absence days * daily rate
  - gross salary = base salary + bonus + overtime
  - total deductions = unpaid absence deduction + advance + manual deduction
  - net salary = gross salary - total deductions
- Admin UI:
  - month/year selector
  - calculate payroll button
  - summary cards
  - filters
  - salary table
  - details breakdown modal
  - finalize and cancel actions
- Employee UI:
  - finalized salary table
  - salary breakdown modal
  - employee dashboard latest net salary card
- Notifications:
  - employee receives a notification when payroll is finalized

## Payslips / Bulletins de paie PDF

- Admin-only PDF generation from finalized payroll calculations
- Employee access to own generated payslips only
- Routes:
  - `/admin/payslips`
  - `/employee/my-payslips`
- Payslip generation rules:
  - requires `PayrollCalculation` status `FINALIZED`
  - draft and cancelled payroll calculations cannot generate payslips
  - regeneration replaces the stored PDF for the same employee/month/year
  - cancelled payslips remain stored but are hidden from employees
- PDF content:
  - RH Paie header
  - month/year
  - employee identity
  - department and job position
  - salary breakdown
  - highlighted net salary
  - academic footer notice
- Local storage:
  - `uploads/payslips`
  - configurable with `FILE_PAYSLIP_UPLOAD_DIR`
- Admin UI:
  - generate payslips by month/year
  - summary cards
  - filters
  - payslip table
  - details modal
  - download PDF
  - cancel payslip
- Employee UI:
  - own generated payslips table
  - PDF download
  - employee dashboard latest payslip card
- Notifications:
  - employee receives a notification when a payslip is generated

## Payroll Exports / Exports de paie

- Admin-only export module
- Route:
  - `/admin/payroll-exports`
- Source data:
  - finalized payroll calculations only
  - draft and cancelled calculations are excluded
- Export preview:
  - employee full name
  - department
  - job position
  - base salary
  - gross salary
  - total deductions
  - net salary
- Summary cards:
  - employee count
  - gross payroll mass
  - net payroll mass
  - deductions
  - bonuses
  - advances
- Excel exports:
  - payroll journal: `journal_paie_YYYY_MM.xlsx`
  - simplified bank transfer file: `virements_paie_YYYY_MM.xlsx`
- CSV export:
  - simplified bank transfer file: `virements_paie_YYYY_MM.csv`
- Employee payment information:
  - RIB
  - bank name
  - payment method
- Bank transfer rows keep employees with missing RIB and mark them with `RIB manquant`
- The bank transfer format is simplified for the academic version and not tied to a specific banking standard

## Demo Data

- Optional backend seed runner
- Disabled by default
- Enabled with:
  - `APPLICATION_SEED_ENABLED=true`
- Creates demonstration data for:
  - admin account
  - employee accounts
  - departments
  - job positions
  - employees
  - leave balances
  - overlapping leave requests for calendar testing
- Demo login:
  - `admin.demo` / `password123`
  - `ali.bensalah` / `password123`

## Frontend UI

- Public landing page
- Login page
- Admin dashboard layout
- Employee dashboard layout
- Admin dashboard summary cards
- Employee dashboard summary cards
- Admin sidebar
- Employee sidebar
- Template-based dashboard styling
- Toast notification system
- Success, error, warning, and info messages
- Backend error messages displayed in UI

## Main Frontend Routes

- `/`
- `/login`
- `/admin/dashboard`
- `/admin/users`
- `/admin/departments`
- `/admin/job-positions`
- `/admin/employees`
- `/admin/contracts`
- `/admin/contracts/monitoring`
- `/admin/employee-exits`
- `/admin/leave-requests`
- `/admin/leave-requests/create`
- `/admin/leave-balances`
- `/admin/leave-calendar`
- `/admin/attendance`
- `/admin/payroll-variables`
- `/admin/payroll-calculations`
- `/admin/payslips`
- `/admin/payroll-exports`
- `/admin/chat-faq`
- `/admin/settings`
- `/employee/dashboard`
- `/employee/my-leave-requests`
- `/employee/leave-requests/create`
- `/employee/leave-calendar`
- `/employee/my-attendance`
- `/employee/my-salaries`
- `/employee/my-payslips`
- `/employee/my-contracts`
- `/employee/my-exit-documents`
- `/employee/profile`

## AI Chatbot / Assistant RH

- Role-aware floating chat widget (admin + employee)
- Local CamemBERT intent classifier (`POST /predict` on port 8010) trained on ~900 RH utterances (home corpus + Avature HR intents mapped/translated)
- Keyword fallback when confidence is low or the ML service is down
- Spring tools over existing RH services (read-only): leave balance, leave requests, payslips, contracts, attendance, leave calendar, salaries, exit documents, pending leaves, expiring contracts, payroll status, exit summary
- Write-action v2 (employee): multi-turn leave draft (« Je veux un congé » → ask days/dates → **Confirmer**) and cancel PENDING leave (`POST /api/chat/actions/leave-request`, `/cancel`, `/discard`)
- Write-action v3 (admin): numbered PENDING leave list → « Accepte 1 2, refuseuse 3 » → **Confirmer** (`POST /api/chat/actions/leave-review`)
- FAQ RAG: bundled markdown + admin-uploaded md/txt/pdf; CamemBERT embeddings + cosine similarity (keyword fallback)
- Admin page **Guide assistant RH** at `/admin/chat-faq`
- Ollama (`llama3.2:3b`) for French phrasing only; never opens the database
- Template fallback when Ollama invents “no data” or drops numbers
- Conversation memory (last 3 turns, year / follow-up reuse)
- Source chip on each reply: Données / Guide / Action / Accès refusé / Hors sujet
- Chat history persisted (`chat_messages`)
- Detailed rapport notes: [`docs/CHATBOT_RAPPORT.md`](docs/CHATBOT_RAPPORT.md)
- Final acceptance / freeze matrix: [`docs/chatbot-acceptance.md`](docs/chatbot-acceptance.md)
- Feature handoff: [`docs/chatbot-handoff.md`](docs/chatbot-handoff.md)

## Backend Technical Features

- Spring Boot 3
- Java 21 bytecode target
- Maven
- PostgreSQL
- Spring Web
- Spring Data JPA
- Spring Security
- Jakarta Validation
- Lombok
- Flyway
- Swagger/OpenAPI
- Global exception handling
- Consistent API response wrapper

## Not Implemented Yet

- Facial recognition
- Chat streaming UI
- Fixed chatbot evaluation set
- Chat write-actions beyond leave create/cancel/admin batch review (run payroll, attendance punch)
- Email notifications
- Password reset UI
