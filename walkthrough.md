# TrackIT Walkthrough

This document explains how the project is structured, how the main flows work, which functions matter, and why the code is shaped this way.

## 1. High-Level Architecture

The application follows a classic layered Java web structure:

```text
Browser
  -> Servlet
  -> Service
  -> DAO
  -> MySQL
  -> DAO
  -> Service
  -> Servlet
  -> JSP view
  -> Browser
```

### Why this structure exists

- Servlets own HTTP concerns: request parameters, sessions, redirects, and forwarding to JSPs.
- Services own business rules: validation, booking conflicts, penalties, return logic, and stock rules.
- DAOs own SQL and row mapping.
- Models carry state between layers.
- JSPs render server-side HTML without mixing SQL into the view layer.

## 2. Main End-to-End Flows

## 2.1 Authentication flow

1. A visitor opens `/login`.
2. `LoginServlet.doGet()` shows the login JSP unless a session already exists.
3. On login submit, `LoginServlet.doPost()` reads email and password.
4. `UserService.login()` fetches the user by email and verifies the BCrypt hash.
5. On success, the servlet stores `loggedInUser` in the session.
6. Admins go to `/admin/dashboard`; regular users go to `/items`.

### Why it works this way

- The session stores the whole logged-in user object so later route checks are cheap.
- BCrypt verification stays in the service layer because it is business/auth logic, not HTTP logic.
- Role-based redirection makes the first landing page match the account type immediately.

## 2.2 Registration flow

1. Visitor opens `/register`.
2. `RegisterServlet.doGet()` shows the form.
3. `RegisterServlet.doPost()` validates required fields, password match, and password length.
4. `UserService.register()` checks email uniqueness, hashes the password, and creates a `USER` record.
5. The user is redirected to `/login?registered=true`.

### Why it works this way

- Public registration is intentionally limited to `USER`.
- Admin accounts are controlled separately and seeded in the schema.

## 2.3 Catalogue flow

1. Logged-in user opens `/items`.
2. `ItemServlet.doGet()` reads `keyword` and `category`.
3. `ItemService.searchAndFilter()` chooses the correct query path.
4. `ItemDAO` runs the actual SQL.
5. `ItemService.decorateAvailability()` enriches each item with reserved-unit counts from `BookingDAO`.
6. The item list JSP renders the cards.

### Why it works this way

- `items.quantity` stores total borrowable units.
- "Available now" is derived, not stored, so it always reflects the latest booking state.

## 2.4 Booking creation flow

1. User clicks Borrow Item on an item card.
2. `BookingServlet.doGet()` with `action=new` loads the item and forwards to `booking-form.jsp`.
3. User submits borrow datetime, return datetime, quantity, and observed condition.
4. `BookingServlet.doPost()` parses the request and calls `BookingService.createBookings()`.
5. `BookingService.createBookings()` validates:
   - dates are present
   - return is not before borrow
   - borrow is not in the past
   - quantity is at least 1
   - item exists and is requestable
   - requested units do not exceed remaining stock for the chosen window
6. One booking row per requested unit is created through `BookingDAO.createAll()`.
7. The user is redirected to `/bookings?success=created`.

### Why it works this way

- The app stores one row per unit instead of one row with quantity `N` because admin handling, return handling, and generated booking references are easier to reason about per unit.
- Pending requests already count against overlapping stock to avoid accepting more demand than the inventory can cover.

## 2.5 Admin approval flow

1. Admin opens `/admin/bookings`.
2. `AdminBookingServlet.doGet()` loads bookings, optionally filtered by keyword and status.
3. Admin clicks Approve or Reject.
4. `AdminBookingServlet.doPost()` dispatches by `action`.
5. `BookingService.updateStatus()` enforces:
   - only `APPROVED` or `REJECTED` are accepted here
   - approvals still have enough remaining stock at approval time
6. `BookingDAO.updateStatus()` persists the new status.

### Why it works this way

- Rechecking stock during approval protects against race conditions between pending requests.
- The servlet does not decide stock rules; the service does.

## 2.6 Return request flow

1. User opens `/bookings`.
2. For approved bookings, the page shows a Return Item action.
3. The return modal asks for the condition on return.
4. `BookingServlet.doPost()` with `action=return` calls `BookingService.requestReturn()`.
5. The service:
   - ensures the booking exists
   - ensures status is `APPROVED`
   - sets `actual_return_date` to now
   - calculates overdue days and RM 5.00 per day penalty
   - moves the booking to `RETURN_PENDING`

### Why it works this way

- User return submission is not the same as final return acceptance.
- Admin still needs to physically inspect the item before the booking closes.

## 2.7 Admin return confirmation flow

1. Admin reviews a `RETURN_PENDING` booking.
2. Admin either confirms the return or rejects it.
3. `BookingService.confirmReturn()`:
   - checks the booking exists and is `RETURN_PENDING`
   - validates damage fee rules
   - combines late fee and damage fee
   - updates item stock/condition/status with `applyInventoryStateAfterReturn()`
   - marks the booking as `RETURNED`
4. `BookingService.rejectReturn()` restores the booking to `APPROVED`.

### Why it works this way

- The app distinguishes "user says it was returned" from "admin has inspected and accepted it".
- Damage handling changes inventory state because a damaged item may no longer be borrowable.

## 2.8 Dashboard flow

1. Admin opens `/admin/dashboard`.
2. `AdminDashboardServlet.doGet()` fetches all items and all bookings.
3. It computes:
   - active bookings
   - overdue bookings
   - pending bookings
   - return-pending bookings
   - returned bookings
   - rejected bookings
   - due-today count
   - total units, reserved units, and available units
   - 7-day borrow trend
   - category stock snapshots
4. These values are set as request attributes and rendered by `dashboard.jsp`.

### Why it works this way

- The dashboard is read-oriented and aggregation-heavy, so building view models in the servlet is simpler than pushing chart logic into the JSP.

## 2.9 Dashboard export flow

1. Admin opens `/admin/dashboard` and clicks Export.
2. The dashboard modal collects:
   - report type: `Dashboard Summary` or `Booking Data`
   - output format: `XLSX` or `PDF`
   - borrow-date range
3. The frontend submits the export request to `/admin/dashboard/export`.
4. `AdminDashboardExportServlet.doGet()` validates the request parameters and builds a `DashboardReportData` snapshot through `DashboardExportService`.
5. `DashboardExportService` filters bookings by borrow date, keeps the inventory snapshot current, and renders the response as either XLSX or PDF.
6. The browser downloads the generated file with a report-specific filename.

### Why it works this way

- `Dashboard Summary` is optimized for management reporting, not raw transaction review.
- `Booking Data` is optimized for audit or spreadsheet analysis.
- The export servlet reuses centralized report-building logic so the same metrics are available across both PDF and XLSX outputs.
- Inventory values remain a current snapshot because stock availability is operational state, not historical borrow-date state.

## 3. Route Protection

`AuthFilter` runs on `/*`.

### What it does

- lets public pages through: `/login`, `/logout`, `/register`
- lets static assets through: `/static/*`, `/css/*`, `/js/*`, `/images/*`
- blocks unauthenticated requests by redirecting to `/login`
- blocks non-admin access to `/admin/*` with HTTP 403

### Why it exists

- It centralizes auth checks so every servlet does not need to repeat session validation.
- It keeps admin-only logic consistent.

## 4. Booking Status Lifecycle

```text
PENDING -> APPROVED -> RETURN_PENDING -> RETURNED
PENDING -> REJECTED
RETURN_PENDING -> APPROVED   (if admin rejects the return request)
```

### Why these states exist

- `PENDING` separates request creation from admin approval.
- `RETURN_PENDING` separates user return submission from admin inspection.
- `RETURNED` is the only fully closed state after admin confirmation.

## 5. Business Rules and Why They Matter

## 5.1 Date validation

- Borrow date cannot be in the past.
- Return date cannot be before borrow date.

Why:

- Prevents invalid reservations and makes penalty logic reliable.

## 5.2 Stock conflict detection

- Overlapping bookings with status `PENDING`, `APPROVED`, or `RETURN_PENDING` count against availability.

Why:

- Pending demand still occupies future capacity.
- Otherwise many users could request the same unit for the same time window and overload admin review.

## 5.3 One booking row per unit

- If a user requests 3 units, the app creates 3 booking rows with quantity `1`.

Why:

- It gives each unit a stable booking reference.
- It simplifies approval and return handling per unit.

## 5.4 Penalty calculation

- Late fee is `daysLate * RM 5.00`.
- Damage fee is only valid when the item was borrowed as `GOOD` and returned as `DAMAGED`.

Why:

- The late fee is deterministic and easy to audit.
- Damage fee rules avoid arbitrary charges.

## 5.5 Inventory after damaged returns

- If a good unit returns damaged and there are still other good units left, the app reduces usable stock instead of freezing the entire item entry.
- If the damage affects the last usable unit, the item may become `UNAVAILABLE`.

Why:

- This is more realistic than treating every unit under the same item record as equally damaged.

## 6. Class-by-Class Walkthrough

The sections below focus on meaningful behavior. Plain getters, setters, constructors, and `toString()` methods are standard data-object helpers and are summarized rather than over-explained.

## 6.1 Utility layer

### `DBConnection`

| Function | What it does | Why |
| --- | --- | --- |
| `getConnection()` | Loads the MySQL driver and creates a fresh JDBC connection using `.env` values | Keeps database access centralized |
| `getEnv(String key)` | Reads config from dotenv or system env, throws a clear error if missing | Fails fast when DB config is incomplete |

## 6.2 Filter layer

### `AuthFilter`

| Function | What it does | Why |
| --- | --- | --- |
| `doFilter(...)` | Allows public/static resources, checks session auth, guards `/admin/*` routes | Centralized security gate |
| `isPublicResource(String path)` | Declares which paths can bypass authentication | Prevents accidental lockout of login/register/static assets |

## 6.3 Servlets

### `LoginServlet`

| Function | What it does | Why |
| --- | --- | --- |
| `doGet(...)` | Shows login page or redirects already-logged-in users by role | Avoids showing login again unnecessarily |
| `doPost(...)` | Validates input, authenticates through `UserService`, creates session | Handles the full login transaction |
| `redirectByRole(...)` | Sends admins to dashboard and users to catalogue | Keeps role-specific entry points clean |

### `RegisterServlet`

| Function | What it does | Why |
| --- | --- | --- |
| `doGet(...)` | Shows registration form | Standard form entry point |
| `doPost(...)` | Validates fields, password rules, and delegates account creation | Keeps registration HTTP logic together |
| `isBlank(String s)` | Small helper for field validation | Avoids repeated null/blank checks |

### `LogoutServlet`

| Function | What it does | Why |
| --- | --- | --- |
| `doGet(...)` | Invalidates the current session and redirects to login | Ends authenticated state cleanly |
| `doPost(...)` | Reuses `doGet()` | Supports both GET and POST logout requests |

### `ItemServlet`

| Function | What it does | Why |
| --- | --- | --- |
| `doGet(...)` | Handles list view, add form, and edit form routes | Central item controller |
| `doPost(...)` | Handles create, update, and delete actions | Central item mutation controller |
| `requireAdmin(...)` | Redirect-based admin guard for GET actions | Prevents non-admin access to admin forms |
| `assertAdmin(...)` | Exception-based admin guard for POST actions | Stops unauthorized item changes |
| `resolveImageUrl(...)` | Chooses uploaded image if present, otherwise keeps existing one | Makes edit behavior predictable |
| `storeItemImage(...)` | Validates image uploads, stores them, mirrors to runtime/project directories | Supports uploaded catalogue images reliably |
| `resolveProjectUploadDirectory()` | Points to `src/main/webapp/static/uploads/items` when running from project root | Keeps local development uploads visible in source tree |
| `resolveRuntimeUploadDirectory(...)` | Resolves the deployed runtime upload directory | Keeps images accessible from the deployed webapp |
| `extractExtension(String fileName)` | Safely normalizes the file extension | Avoids unsafe or malformed filenames |
| `samePath(Path left, Path right)` | Prevents duplicate upload targets | Avoids redundant copy operations |
| `buildItemFromRequest(...)` | Rebuilds form state after validation errors | Preserves user input on failed submissions |

### `BookingServlet`

| Function | What it does | Why |
| --- | --- | --- |
| `doGet(...)` | Shows booking form or lists the logged-in user's bookings | User booking controller |
| `doPost(...)` | Creates bookings or submits return requests | Handles the two user write actions |
| `loggedInUser(...)` | Reads the session user object | Small convenience helper |
| `reloadBookingForm(...)` | Reloads the chosen item when booking creation fails | Prevents a broken form after validation errors |
| `buildBookingsRedirect(...)` | Rebuilds redirects while preserving filters | Better UX after actions |
| `encode(String value)` | URL-encodes query parameters | Safe redirect construction |

### `AdminDashboardServlet`

| Function | What it does | Why |
| --- | --- | --- |
| `doGet(...)` | Loads items/bookings, computes dashboard metrics, forwards to dashboard JSP | Single read-only admin summary endpoint |
| `buildBorrowTrend(...)` | Builds a 7-day borrow count series | Powers the dashboard trend chart |
| `buildCategorySnapshots(...)` | Aggregates category totals, reserved units, and available units | Powers category stock visualization |

### `AdminDashboardExportServlet`

| Function | What it does | Why |
| --- | --- | --- |
| `doGet(...)` | Validates report type, format, and date range, then streams the requested export file | Single admin export entry point |
| `normalizeReportType(String value)` | Restricts exports to `summary` or `data` | Prevents unsupported report requests |
| `normalizeFormat(String value)` | Restricts output to `xlsx` or `pdf` | Keeps export handling explicit |
| `parseDate(String value, String missingMessage)` | Parses required `fromDate` and `toDate` inputs | Gives clear validation errors for bad date input |
| `buildFileName(...)` | Generates download-friendly filenames | Makes exports easier to organize |
| `sendError(...)` | Returns plain-text export errors to the browser | Supports frontend toast/error handling |

### `AdminBookingServlet`

| Function | What it does | Why |
| --- | --- | --- |
| `doGet(...)` | Loads filtered bookings for admin review | Admin booking list entry point |
| `doPost(...)` | Approves, rejects, confirms returns, or rejects returns | Admin booking action dispatcher |
| `parseMoney(String value)` | Parses optional damage-fee input safely | Keeps money parsing explicit |
| `buildBookingsRedirect(...)` | Preserves keyword/status filters after admin actions | Better list workflow |
| `encode(String value)` | URL-encodes redirect query values | Safe redirect construction |

## 6.4 Services

### `UserService`

| Function | What it does | Why |
| --- | --- | --- |
| `login(String email, String password)` | Loads the user by email and verifies the BCrypt hash | Authentication belongs in the service layer |
| `register(String name, String email, String password)` | Enforces unique email, hashes password, stores `USER` account | Registration rules belong in one place |

### `ItemService`

| Function | What it does | Why |
| --- | --- | --- |
| `getAllItems()` | Fetches all items and decorates reserved/available counts | Ensures views get stock-aware item models |
| `searchAndFilter(...)` | Selects the correct DAO query based on keyword/category presence | Keeps controller logic simple |
| `getById(int id)` | Loads one item or throws if missing, then decorates availability | Prevents null handling from leaking upward |
| `getCategories()` | Loads distinct item categories | Supports catalogue filter UI |
| `createItem(...)` | Validates and creates a new item with default status/condition | Consistent item creation rules |
| `updateItem(...)` | Validates and updates an existing item | Consistent admin edit rules |
| `deleteItem(int id)` | Deletes an item | Thin service wrapper for symmetry |
| `countAll()` | Counts items | Dashboard support |
| `validate(...)` | Validates name, category, and quantity | Shared item input rules |
| `normalizeStatus(String status)` | Restricts item status to valid enum-like values | Protects DB state |
| `normalizeCondition(String condition)` | Restricts item condition to valid enum-like values | Protects DB state |
| `normalizeImageUrl(String imageUrl)` | Trims or nulls image URL values | Avoids garbage strings |
| `decorateAvailability(List<Item> items)` | Adds reserved units to each item from booking data | Makes stock numbers accurate |
| `decorateAvailability(Item item)` | Same as above for a single item | Needed by detail/form flows |

### `BookingService`

| Function | What it does | Why |
| --- | --- | --- |
| `createBookings(...)` | Validates booking rules, checks overlap-aware stock, creates one row per unit | Core booking creation logic |
| `updateStatus(int bookingId, String newStatus)` | Approves or rejects a pending booking and rechecks stock when approving | Prevents invalid approvals |
| `requestReturn(int bookingId, String conditionAfter)` | Moves an approved booking to `RETURN_PENDING`, records return timestamp and late fee | Handles the user return request workflow |
| `confirmReturn(int bookingId, BigDecimal damageFee)` | Finalizes a return, validates damage fee, updates inventory, closes booking | Core admin return logic |
| `rejectReturn(int bookingId)` | Restores a return request back to `APPROVED` | Lets admin reject a bad/incomplete return submission |
| `getAllBookings()` | Loads all bookings | Admin and dashboard support |
| `getAllBookingsFiltered(...)` | Loads filtered admin bookings | Admin list support |
| `getBookingsByUser(int userId)` | Loads one user's bookings | User booking history support |
| `getBookingsByUserFiltered(...)` | Loads filtered user bookings | User list support |
| `getBookingById(int id)` | Loads one booking | Utility lookup |
| `countActive()` | Counts active bookings | Dashboard support |
| `countOverdue()` | Counts overdue approved bookings | Dashboard support |
| `normalizeCondition(String condition)` | Restricts booking condition values to `GOOD` or `DAMAGED` | Keeps condition state clean |
| `normalizeDamageFee(...)` | Validates when a damage fee is allowed and when it is required | Prevents arbitrary fee input |
| `applyInventoryStateAfterReturn(...)` | Updates stock, status, and condition after a confirmed return | Encodes the inventory-damage rule |
| `normalizeKeywordLike(String keyword)` | Converts a search term into a SQL `LIKE` pattern | Shared filter behavior |
| `extractBookingId(String keyword)` | Parses values like `BK-0004` into integer booking IDs | Lets search accept human-readable references |
| `normalizeStatus(String status)` | Normalizes status filter input | Shared filtering helper |

### `DashboardExportService`

| Function | What it does | Why |
| --- | --- | --- |
| `buildReportData(LocalDate fromDate, LocalDate toDate)` | Builds a reusable export snapshot with filtered booking metrics, trend points, category stock, and booking rows | Keeps export aggregation in one place |
| `writeSummaryXlsx(...)` | Generates the dashboard summary workbook | Shareable spreadsheet export |
| `writeBookingDataXlsx(...)` | Generates the detailed booking workbook | Spreadsheet-friendly raw data export |
| `writeSummaryPdf(...)` | Generates the dashboard summary PDF | Printable/shareable management report |
| `writeBookingDataPdf(...)` | Generates the booking-data PDF | Printable detailed report |
| `filterBookingsByBorrowDate(...)` | Keeps only bookings whose borrow date falls inside the selected range | Makes export filtering consistent |
| `buildBookingTrend(...)` | Builds a date-by-date series for the selected range | Supports trend output in both PDF and XLSX |
| `buildCategorySnapshots(...)` | Aggregates current stock by category | Reuses dashboard-style stock reporting |
| `validateDateRange(...)` | Rejects missing or reversed date ranges | Prevents invalid exports |

## 6.5 DAOs

### `UserDAO`

| Function | What it does | Why |
| --- | --- | --- |
| `findByEmail(String email)` | Loads one user by email | Login needs it |
| `findById(int id)` | Loads one user by ID | General lookup |
| `countAll()` | Counts users | Dashboard metric |
| `findAll()` | Loads every user | Admin/reporting use |
| `create(User user)` | Inserts a user and fills generated ID | Registration persistence |
| `mapRow(ResultSet rs)` | Converts a DB row to `User` | Central mapping helper |

### `ItemDAO`

| Function | What it does | Why |
| --- | --- | --- |
| `findAll()` | Loads all items ordered by name | Default catalogue query |
| `searchByName(String keyword)` | Name search | Catalogue search |
| `filterByCategory(String category)` | Exact category filter | Catalogue filter |
| `searchAndFilter(String keyword, String category)` | Combines both filters | Catalogue combined filter |
| `findById(int id)` | Loads one item | Detail/edit/booking forms |
| `findAllCategories()` | Loads distinct categories | Filter dropdown |
| `countAll()` | Counts items | Dashboard metric |
| `create(Item item)` | Inserts an item and fills generated ID | Admin item creation |
| `update(Item item)` | Updates an item | Admin edits |
| `delete(int id)` | Deletes an item | Admin removal |
| `queryItems(...)` | Shared query helper for 0, 1, or 2 text parameters | Reduces repeated JDBC code |
| `mapRow(ResultSet rs)` | Converts a DB row to `Item` | Central mapping helper |

### `BookingDAO`

| Function | What it does | Why |
| --- | --- | --- |
| `findAll()` | Loads all bookings with joined user/item names | Admin and dashboard views |
| `findByUserId(int userId)` | Loads one user's bookings | User history |
| `findAllFiltered(...)` | Loads filtered admin bookings | Admin search/filter |
| `findByUserIdFiltered(...)` | Loads filtered user bookings | User search/filter |
| `findById(int id)` | Loads one booking with names | Workflow lookup |
| `create(Booking b)` | Inserts one booking by calling `createAll()` | Convenience method |
| `createAll(List<Booking> bookings)` | Batch-inserts bookings in one transaction | Supports one-row-per-unit creation |
| `updateStatus(int bookingId, String newStatus)` | Updates status only | Simple state changes |
| `requestReturn(...)` | Stores return timestamp, penalty, and condition, then sets `RETURN_PENDING` | Return request persistence |
| `confirmReturn(...)` | Sets `RETURNED` and final penalty | Admin return confirmation persistence |
| `rejectReturn(int bookingId)` | Restores `APPROVED`, clears return fields and penalty | Admin return rejection persistence |
| `countOverlappingOpenBookings(...)` | Counts overlapping units in `PENDING`, `APPROVED`, and `RETURN_PENDING` | Core stock-conflict check |
| `hasConflict(...)` | Legacy boolean wrapper over overlap counting | Compatibility helper |
| `countReservedUnits()` | Groups reserved units per item for approved/return-pending bookings | Drives availability and dashboard numbers |
| `countReservedUnitsForItem(int itemId)` | Gets reserved units for one item | Single-item availability logic |
| `countActive()` | Counts active bookings | Dashboard metric |
| `countOverdue()` | Counts overdue approved bookings | Dashboard metric |
| `appendFilters(...)` | Builds keyword, booking-id, and status SQL filters | Shared filtering logic |
| `query(String sql, List<Object> params)` | Generic query executor and binder | Reuses JDBC logic |
| `mapRow(ResultSet rs)` | Converts a joined row into `Booking` | Central mapping helper |

## 6.6 Models

### `User`

- Constructors and getters/setters expose the basic user fields used by DAO mapping, sessions, and JSPs.
- `isAdmin()` is the meaningful helper; it centralizes the role check.
- `toString()` helps with debugging.

### `Item`

- Constructors and getters/setters expose the persisted item fields.
- `unitsInUse` / `reservedUnits` are derived runtime fields, not separate DB columns.
- `getUnitsAvailableNow()` and `getAvailableUnits()` compute available stock.
- `isAvailable()` checks the item status.
- `isRequestable()` checks status plus quantity.
- `hasUnitsAvailableNow()` / `hasAvailableUnits()` are convenient view helpers.
- `toString()` helps with debugging.

### `Booking`

- Constructors and getters/setters expose booking state plus display-only joined fields.
- `isApproved()`, `isReturned()`, and `isReturnPending()` simplify JSP conditions.
- `isLate()` checks whether a penalty exists.
- `getReferenceCode()` converts the numeric ID into the display format `BK-0001`.
- `toString()` helps with debugging.

### `CategoryStockSnapshot`

- Constructor and getters hold pre-aggregated dashboard stock data by category.
- Why: the JSP should render a simple view model, not do grouping logic itself.

### `DashboardTrendPoint`

- Constructor and getters hold a label/value pair for the 7-day booking trend.
- Why: the dashboard chart needs a small dedicated view model.

### `DashboardReportData`

- Constructor and getters hold the reusable export snapshot used by PDF and XLSX generation.
- It combines the selected borrow-date range, current inventory totals, booking status counts, trend data, category stock, and optional detailed booking rows.
- `getApprovedBookings()` derives the approved-active figure by excluding return-pending rows from the broader active total.

## 6.7 Frontend JavaScript (`app.js`)

The frontend script is intentionally small and utility-driven.

| Function | What it does | Why |
| --- | --- | --- |
| startup `DOMContentLoaded` block | Initializes theme, mobile nav, alert toasts, image preview, and admin confirmations | Central boot sequence |
| `initTheme()` | Restores theme from `localStorage` or defaults to light | Theme persistence |
| `toggleTheme()` | Switches between dark and light theme | User preference control |
| `updateThemeIcon()` | Updates the theme button label/title | Better UX |
| `getToastContainer()` | Creates or reuses the toast wrapper | Keeps toast rendering centralized |
| `showToast(...)` | Renders a timed toast | Replaces static inline alerts |
| `removeToast(toast)` | Animates and removes a toast | Cleanup |
| `initAlertToasts()` | Converts server-rendered `.alert` messages into toasts | Keeps server-side feedback but improves presentation |
| `openModal(id)` | Shows a modal and focuses the first input | Shared modal opening behavior |
| `closeModal(id)` | Hides a modal with animation | Shared modal closing behavior |
| overlay click handler | Closes modals when clicking outside them | Standard modal UX |
| Escape key handler | Closes open modals with Escape | Accessibility and convenience |
| `initMobileNav()` | Toggles mobile navigation links | Responsive navigation |
| `togglePassword(inputId)` | Toggles password field visibility | Better form usability |
| `showReturnModal(bookingId)` | Opens the return modal and sets the booking ID | User return flow |
| `hideReturnModal()` | Closes the return modal | UI helper |
| `initAdminConfirmations()` | Intercepts admin forms and routes them through the shared confirm modal | Consistent confirmation UX |
| `showAdminConfirmModal(form)` | Populates the confirm modal, including damage-fee rules | Reduces duplicated modal markup |
| `hideConfirmModal()` | Closes the confirm modal and clears pending form state | UI helper |
| `initDashboardExportModal()` | Wires the export modal fields, date bounds, and download request flow | Keeps export UI behavior centralized |
| `showDashboardExportModal()` | Opens the dashboard export modal | Dashboard action helper |
| `hideDashboardExportModal()` | Closes the export modal | Dashboard action helper |
| `extractDownloadFilename(...)` | Parses a filename from the response headers | Preserves meaningful download names |
| `buildFallbackExportFilename(...)` | Builds a safe fallback filename when headers are missing | Prevents awkward browser downloads |
| `initItemSearch()` | Client-side item search helper | Optional fast filtering helper |
| `initImagePreview()` | Previews uploaded images in the item form | Better admin item-edit UX |

## 7. Why the Code Is Structured This Way

The project favors simple, explicit code over abstraction-heavy code:

- no ORM; JDBC is used directly so SQL is visible
- no REST API layer; servlets render JSPs directly
- no frontend framework; small UI helpers live in one JavaScript file
- business rules are concentrated in services so servlets stay readable

That makes the code easier to demo, teach, and debug in a classroom or small-project setting.
