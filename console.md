2025-11-25T14:45:07.384Z INFO 11628 --- [arc-raiders-planner] [nio-8080-exec-1] o.s.web.servlet.DispatcherServlet : Completed initialization in 1 ms
Hibernate:
select
i1_0.id,
i1_0.description,
i1_0.icon_url,
i1_0.item_type,
i1_0.loot_type_id,
i1_0.name,
i1_0.rarity,
i1_0.stack_size,
i1_0.value,
i1_0.weight
from
items i1_0
where
i1_0.name=?
Hibernate:
select
lt1_0.id,
lt1_0.description,
lt1_0.name
from
loot_types lt1_0
where
lt1_0.id=?
Hibernate:
select
mm1_0.id,
mm1_0.category,
mm1_0.map_id,
mm1_0.lat,
mm1_0.lng,
mm1_0.name,
mm1_0.subcategory
from
map_markers mm1_0
where
upper(mm1_0.category)=upper(?)
Hibernate:
select
gm1_0.id,
gm1_0.cal_offset_x,
gm1_0.cal_offset_y,
gm1_0.cal_scale_x,
gm1_0.cal_scale_y,
gm1_0.description,
gm1_0.image_url,
gm1_0.name
from
maps gm1_0
where
gm1_0.id=?
Hibernate:
select
gm1_0.id,
gm1_0.cal_offset_x,
gm1_0.cal_offset_y,
gm1_0.cal_scale_x,
gm1_0.cal_scale_y,
gm1_0.description,
gm1_0.image_url,
gm1_0.name
from
maps gm1_0
where
gm1_0.id=?
Hibernate:
select
gm1_0.id,
gm1_0.cal_offset_x,
gm1_0.cal_offset_y,
gm1_0.cal_scale_x,
gm1_0.cal_scale_y,
gm1_0.description,
gm1_0.image_url,
gm1_0.name
from
maps gm1_0
where
gm1_0.id=?
Hibernate:
select
gm1_0.id,
gm1_0.cal_offset_x,
gm1_0.cal_offset_y,
gm1_0.cal_scale_x,
gm1_0.cal_scale_y,
gm1_0.description,
gm1_0.image_url,
gm1_0.name
from
maps gm1_0
where
gm1_0.id=?
Hibernate:
select
distinct gm1_0.id,
a1_0.map_id,
a1_0.id,
a1_0.coordinates,
a1_0.loot_abundance,
lt1_0.area_id,
lt1_1.id,
lt1_1.description,
lt1_1.name,
a1_0.map_x,
a1_0.map_y,
a1_0.name,
gm1_0.cal_offset_x,
gm1_0.cal_offset_y,
gm1_0.cal_scale_x,
gm1_0.cal_scale_y,
gm1_0.description,
gm1_0.image_url,
gm1_0.name
from
maps gm1_0
left join
areas a1_0
on gm1_0.id=a1_0.map_id
left join
area_loot_type lt1_0
on a1_0.id=lt1_0.area_id
left join
loot_types lt1_1
on lt1_1.id=lt1_0.loot_type_id
Hibernate:
select
mm1_0.id,
mm1_0.category,
mm1_0.map_id,
mm1_0.lat,
mm1_0.lng,
mm1_0.name,
mm1_0.subcategory
from
map_markers mm1_0
left join
maps gm1_0
on gm1_0.id=mm1_0.map_id
where
gm1_0.id=?
2025-11-25T14:45:07.411Z DEBUG 11628 --- [arc-raiders-planner] [nio-8080-exec-1] c.p.a.service.PlannerService : Using Raider Hatches: found 4 hatches for map Blue Gate
2025-11-25T14:45:07.412Z DEBUG 11628 --- [arc-raiders-planner] [nio-8080-exec-1] c.p.a.service.PlannerService : Generated route with 1 areas
2025-11-25T14:45:07.412Z DEBUG 11628 --- [arc-raiders-planner] [nio-8080-exec-1] c.p.a.service.PlannerService : Selected extraction: Extraction Point at [157.01221123725236, 878.6082312050498], distance: 356.05898250067776
2025-11-25T14:45:07.413Z DEBUG 11628 --- [arc-raiders-planner] [nio-8080-exec-1] c.p.a.service.PlannerService : Route for Blue Gate: extraction=Extraction Point, coords=[157.01221123725236, 878.6082312050498]
Hibernate:
select
mm1_0.id,
mm1_0.category,
mm1_0.map_id,
mm1_0.lat,
mm1_0.lng,
mm1_0.name,
mm1_0.subcategory
from
map_markers mm1_0
left join
maps gm1_0
on gm1_0.id=mm1_0.map_id
where
gm1_0.id=?
2025-11-25T14:45:07.418Z DEBUG 11628 --- [arc-raiders-planner] [nio-8080-exec-1] c.p.a.service.PlannerService : Using Raider Hatches: found 4 hatches for map Dam Battlegrounds
2025-11-25T14:45:07.418Z DEBUG 11628 --- [arc-raiders-planner] [nio-8080-exec-1] c.p.a.service.PlannerService : Generated route with 3 areas
2025-11-25T14:45:07.418Z DEBUG 11628 --- [arc-raiders-planner] [nio-8080-exec-1] c.p.a.service.PlannerService : Selected extraction: Good Old Baron's Hatch at [-255.82385208620735, -355.67807965926886], distance: 193.8382615007945
2025-11-25T14:45:07.419Z DEBUG 11628 --- [arc-raiders-planner] [nio-8080-exec-1] c.p.a.service.PlannerService : Route for Dam Battlegrounds: extraction=Good Old Baron's Hatch, coords=[-255.82385208620735, -355.67807965926886]
Hibernate:
select
mm1_0.id,
mm1_0.category,
mm1_0.map_id,
mm1_0.lat,
mm1_0.lng,
mm1_0.name,
mm1_0.subcategory
from
map_markers mm1_0
left join
maps gm1_0
on gm1_0.id=mm1_0.map_id
where
gm1_0.id=?
2025-11-25T14:45:07.422Z DEBUG 11628 --- [arc-raiders-planner] [nio-8080-exec-1] c.p.a.service.PlannerService : Using Raider Hatches: found 4 hatches for map The Spaceport
2025-11-25T14:45:07.422Z DEBUG 11628 --- [arc-raiders-planner] [nio-8080-exec-1] c.p.a.service.PlannerService : No viable areas found - returning fallback extraction point if available
2025-11-25T14:45:07.423Z DEBUG 11628 --- [arc-raiders-planner] [nio-8080-exec-1] c.p.a.service.PlannerService : Route for The Spaceport: extraction=null, coords=[null, null]
Hibernate:
select
mm1_0.id,
mm1_0.category,
mm1_0.map_id,
mm1_0.lat,
mm1_0.lng,
mm1_0.name,
mm1_0.subcategory
from
map_markers mm1_0
left join
maps gm1_0
on gm1_0.id=mm1_0.map_id
where
gm1_0.id=?
2025-11-25T14:45:07.426Z DEBUG 11628 --- [arc-raiders-planner] [nio-8080-exec-1] c.p.a.service.PlannerService : Using Raider Hatches: found 4 hatches for map Buried City
2025-11-25T14:45:07.426Z DEBUG 11628 --- [arc-raiders-planner] [nio-8080-exec-1] c.p.a.service.PlannerService : No viable areas found - returning fallback extraction point if available
2025-11-25T14:45:07.426Z DEBUG 11628 --- [arc-raiders-planner] [nio-8080-exec-1] c.p.a.service.PlannerService : Route for Buried City: extraction=null, coords=[null, null]
Hibernate:
select
gm1_0.id,
gm1_0.cal_offset_x,
gm1_0.cal_offset_y,
gm1_0.cal_scale_x,
gm1_0.cal_scale_y,
gm1_0.description,
gm1_0.image_url,
gm1_0.name
from
maps gm1_0
Hibernate:
select
distinct gm1_0.id,
a1_0.map_id,
a1_0.id,
a1_0.coordinates,
a1_0.loot_abundance,
lt1_0.area_id,
lt1_1.id,
lt1_1.description,
lt1_1.name,
a1_0.map_x,
a1_0.map_y,
a1_0.name,
gm1_0.cal_offset_x,
gm1_0.cal_offset_y,
gm1_0.cal_scale_x,
gm1_0.cal_scale_y,
gm1_0.description,
gm1_0.image_url,
gm1_0.name
from
maps gm1_0
left join
areas a1_0
on gm1_0.id=a1_0.map_id
left join
area_loot_type lt1_0
on a1_0.id=lt1_0.area_id
left join
loot_types lt1_1
on lt1_1.id=lt1_0.loot_type_id
where
gm1_0.name=?
