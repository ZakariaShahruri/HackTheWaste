-- A WhatsApp report can start with either half: the pin or the photo.
alter table pending_report alter column lat drop not null;
alter table pending_report alter column lng drop not null;
alter table pending_report add column media_url varchar(1024);
alter table pending_report add constraint pending_report_pin_or_photo check (
    (lat is not null and lng is not null and media_url is null)
    or (lat is null and lng is null and media_url is not null));
