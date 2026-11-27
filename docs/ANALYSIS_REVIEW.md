# Analysis review: ERD and business logic fixes

## ERD problems found and how the code fixes them
1. **Team.project_id** tied a team to a single project and "User belongs to Team (M:1)" allowed only one team per user.
   Now: `team_members` (user <-> team, many to many) and `project_teams` / `project_members` (project <-> team / user, many to many).
2. **"User manages Task (1:1)"** is wrong. A user has many tasks. Now: `tasks.assigned_to` (N:1 to user) and `tasks.created_by`.
   The Team to Task line had no foreign key; removed. A task belongs to a project.
3. **Chat message** had only sender and receiver, but the screens have project group chats and announcements.
   Now: `channel` (DIRECT, PROJECT, ANNOUNCEMENT), nullable `receiver_id`, nullable `project_id`.
4. **Task status** was undefined. Defined as PENDING, IN_PROGRESS, REVIEWING, ACCEPTED, DONE, with who may do each move
   (see README). Added `rating` and `review_feedback` to tasks (the screens show quality ratings and rework notes).
5. **Missing entities used by the screens:** `attachments` (project files, task guidelines, deliverables), `notifications`,
   `password_reset_tokens`. Added `created_at` timestamps.
6. **Password** column is now `password_hash` (BCrypt).
7. **Roles:** ERD said Manager / Team Leader / Member, screens say Admin / Member, requirements say manager or member.
   Implemented ADMIN (manager) and MEMBER.

## Business logic problems
- A member could be removed from a project while keeping open tasks. Now their open tasks are unassigned and set back to Pending.
- Tasks can only be assigned to people in the project.
- Only the project's own manager can edit it, add members, create tasks, review. Members only see their own projects.
- Accepting needs a rating (1-5); rejecting needs feedback.

## Document inconsistencies (not code)
- Timeline says "Node.js setup", the business model says React, Node.js and MongoDB. The stack is Spring Boot, MySQL and Angular.
- Non-functional requirement 2 says chat latency "should be above 200 ms". It should be below.
- "Team Leader" role appears only in the ERD.
