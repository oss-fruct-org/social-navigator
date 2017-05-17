#!/bin/bash
# Синтезатор речи на основе технологий Яндекса.
# В качестве параметра скрипту передается текст для воспроизведения в 'кавычках'.
# Пример запроса: ./yatts 'Привет Ubuntu'
# Произношение отличается в зависимости от наличия знаков препинания.
# 
# В ссылке запроса можно изменить следующие параметры:
# format - mp3 или wav
# quality - lo или hi

tts_ru () {
echo "convert === $1 ($2) ==="
wget -O ru/$2.mp3 -o /dev/null "http://tts.voicetech.yandex.net/generate?format=mp3&quality=hi&lang=ru_RU&text=$1&speaker=oksana&key=25b5d8c5-0088-4c48-8416-787e703a10d5"
}

tts_en () {
echo "convert === $1 ($2) ==="
wget -O en/$2.mp3 -o /dev/null "http://tts.voicetech.yandex.net/generate?format=mp3&quality=hi&lang=en_US&text=$1&speaker=oksana&key=25b5d8c5-0088-4c48-8416-787e703a10d5"
}

rm ru/*
# направление
tts_ru "Сзади" "back"
tts_ru "Справа" "right"
tts_ru "Слева" "left"
tts_ru "Впереди" "forward"

# движение
tts_ru "Поворот" "turn"
tts_ru "Налево" "turn_left"
tts_ru "Направо" "turn_right"

# препятствие
tts_ru "Лестница" "stairs"
tts_ru "Помеха на дороге" "object_on_the_road"
tts_ru "Остановка транспорта" "bus_stop"
tts_ru "Ворота" "gate"
tts_ru "Пандус" "ramp"
tts_ru "Уклон дороги" "slope"
tts_ru "Узкая дорога" "narrow_road"
tts_ru "Неровная дорога" "rough_road"
tts_ru "Светофор" "traffic_light"
tts_ru "Пешеходный переход" "crosswalk"
tts_ru "Бордюр" "curb"

# сервисные функции
tts_ru "Неизвестное направление" "unknown_dir"  
tts_ru "Что-то" "unknown_obj"
tts_ru "Включено аудио сопровождение" "welcome"

############################### EN #################

rm -f en/*
# направление
tts_en "behind" "back"
tts_en "left" "left"
tts_en "right" "right"
tts_en "ahead" "forward"

# движение
tts_en "turn" "turn"
tts_en "to the left" "turn_left"
tts_en "to the right" "turn_right"

# препятствие
tts_en "crosswalk" "crosswalk"
tts_en "ramp" "ramp"
tts_en "rough road" "rough_road"
tts_en "Slope of the road" "slope"
tts_en "Narrow road" "narrow_road"
tts_en "gate" "gate"
tts_en "stairs" "stairs"
tts_en "traffic light" "traffic_light"
tts_en "curbstone" "curb"
tts_en "o+bject on the road" "object_on_the_road"
tts_en "bus stop" "bus_stop"

# сервисные функции
tts_en "unknown direction" "unknown_dir"  
tts_en "Something" "unknown_obj"
tts_en "Audio tracking is turned on" "welcome"
