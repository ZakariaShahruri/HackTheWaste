-- When the sender was last shown the clan list, so their next message can be the pick.
alter table whatsapp_sender add column choosing_since timestamp(6) with time zone;
