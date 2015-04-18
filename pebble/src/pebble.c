#include <pebble.h>
	
#define KEY_BTN 5
#define KEY_STATE 4
#define KEY_TIME 3

static Window *s_main_window;
static TextLayer *s_time_layer, *s_error_layer;

static time_t first_timestamp = 0;
static time_t sum_time = 0;

char *translate_error(AppMessageResult result) {
  switch (result) {
	case APP_MSG_OK: return "APP_MSG_OK";
	case APP_MSG_SEND_TIMEOUT: return "APP_MSG_SEND_TIMEOUT";
	case APP_MSG_SEND_REJECTED: return "APP_MSG_SEND_REJECTED";
	case APP_MSG_NOT_CONNECTED: return "APP_MSG_NOT_CONNECTED";
	case APP_MSG_APP_NOT_RUNNING: return "APP_MSG_APP_NOT_RUNNING";
	case APP_MSG_INVALID_ARGS: return "APP_MSG_INVALID_ARGS";
	case APP_MSG_BUSY: return "APP_MSG_BUSY";
	case APP_MSG_BUFFER_OVERFLOW: return "APP_MSG_BUFFER_OVERFLOW";
	case APP_MSG_ALREADY_RELEASED: return "APP_MSG_ALREADY_RELEASED";
	case APP_MSG_CALLBACK_ALREADY_REGISTERED: return "APP_MSG_CALLBACK_ALREADY_REGISTERED";
	case APP_MSG_CALLBACK_NOT_REGISTERED: return "APP_MSG_CALLBACK_NOT_REGISTERED";
	case APP_MSG_OUT_OF_MEMORY: return "APP_MSG_OUT_OF_MEMORY";
	case APP_MSG_CLOSED: return "APP_MSG_CLOSED";
	case APP_MSG_INTERNAL_ERROR: return "APP_MSG_INTERNAL_ERROR";
	default: return "UNKNOWN ERROR";
  }
}

static void update_time() {

	time_t temp = time(NULL) - first_timestamp + sum_time; 
	struct tm *tick_time = localtime(&temp);
 
	static char buffer[] = "00:00";
	strftime(buffer, sizeof("00:00"), "%M:%S", tick_time);
	text_layer_set_text(s_time_layer, buffer);
}
 
static void main_window_load(Window *window) {

	s_time_layer = text_layer_create(GRect(0, 55, 144, 50));

	text_layer_set_background_color(s_time_layer, GColorClear);
	text_layer_set_text_color(s_time_layer, GColorBlack);
	text_layer_set_text(s_time_layer, "00:00");
	text_layer_set_font(s_time_layer, fonts_get_system_font(FONT_KEY_BITHAM_42_BOLD));
	text_layer_set_text_alignment(s_time_layer, GTextAlignmentCenter);
 
	layer_add_child(window_get_root_layer(window), text_layer_get_layer(s_time_layer));

	s_error_layer = text_layer_create(GRect(0, 0, 144, 50));
	text_layer_set_text_color(s_error_layer, GColorBlack);
	layer_add_child(window_get_root_layer(window), text_layer_get_layer(s_error_layer));

}
 
static void main_window_unload(Window *window) {
	text_layer_destroy(s_time_layer);
}
 
static void tick_handler(struct tm *tick_time, TimeUnits units_changed) {
	update_time();
}


static void start(int time_offset) {
	text_layer_set_text(s_error_layer, "Running");
	first_timestamp = time(NULL) - time_offset / 1000;
	tick_timer_service_subscribe(SECOND_UNIT, tick_handler);
	update_time();
}

static void pause(int time_offset) {
	text_layer_set_text(s_error_layer, "Paused");
	first_timestamp = time(NULL) - time_offset / 1000;
	tick_timer_service_unsubscribe();
	update_time();
}

static void reset() {
	// tick_timer_service_unsubscribe();
	// first_timestamp = time(NULL);
	// update_time();
}
	
static void select_click_handler(ClickRecognizerRef recognizer, void *context) {
	
	DictionaryIterator *iter;
    app_message_outbox_begin(&iter);
    dict_write_int8(iter, KEY_BTN, 1);
    dict_write_end(iter);
    app_message_outbox_send();

}

static void select_longclick_handler(ClickRecognizerRef recognizer, void *context) {
	reset();
}

static void click_config_provider(void *context) {
	window_single_click_subscribe(BUTTON_ID_SELECT, select_click_handler);
	window_long_click_subscribe(BUTTON_ID_SELECT, 500, select_longclick_handler, NULL);
}


static void inbox_received_callback(DictionaryIterator *iterator, void *context) {
  	APP_LOG(APP_LOG_LEVEL_INFO, "Message received!");

	//Get the first pair
	Tuple *t = dict_read_first(iterator);

	char* state = NULL;
	uint32_t time_offset = 0;

	// Process all pairs present
	while (t != NULL) {

		// Long lived buffer
		static char s_buffer[64];

		// Process this pair's key
		switch (t->key) {

			case KEY_STATE:
  				state = t->value->cstring;				
				break;

			case KEY_TIME:
				time_offset = t->value->uint32;
				snprintf(s_buffer, sizeof(s_buffer), "Received %lu", t->value->uint32);
  				APP_LOG(APP_LOG_LEVEL_INFO, s_buffer);
				break;
		}

		// Get next pair, if any
		t = dict_read_next(iterator);
	}

	APP_LOG(APP_LOG_LEVEL_INFO, state);
	
	if(strcmp(state, "started") == 0)
		start(time_offset);
	else if(strcmp(state, "paused") == 0)
		pause(time_offset);


}

static void inbox_dropped_callback(AppMessageResult reason, void *context) {
  APP_LOG(APP_LOG_LEVEL_ERROR, "Message dropped!");
}


static void outbox_failed_callback(DictionaryIterator *iterator, AppMessageResult reason, void *context) {
	APP_LOG(APP_LOG_LEVEL_ERROR, "Outbox send failed!");
}

static void outbox_sent_callback(DictionaryIterator *iterator, void *context) {
	APP_LOG(APP_LOG_LEVEL_INFO, "Outbox send success!");
}

static void init() {

	app_message_register_inbox_received(inbox_received_callback);
	app_message_register_inbox_dropped(inbox_dropped_callback);
	app_message_register_outbox_failed(outbox_failed_callback);
	app_message_register_outbox_sent(outbox_sent_callback);

	// Open AppMessage
	app_message_open(app_message_inbox_size_maximum(), app_message_inbox_size_maximum());

	// Create main Window element and assign to pointer
	s_main_window = window_create();
 
	// Set handlers to manage the elements inside the Window
	window_set_window_handlers(s_main_window, (WindowHandlers) {
		.load = main_window_load,
		.unload = main_window_unload
	});
 
	// Show the Window on the watch, with animated=true
	window_stack_push(s_main_window, true);
	
	window_set_click_config_provider(s_main_window, click_config_provider);



}
 
static void deinit() {

	// Destroy Window
	window_destroy(s_main_window);
}
 
int main(void) {
	init();
	app_event_loop();
	deinit();
}