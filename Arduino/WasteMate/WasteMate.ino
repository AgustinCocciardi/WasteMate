#include <PWMServo.h>
#include <Wire.h>
#include <SoftwareSerial.h>
#include "rgb_lcd.h"
#include <ArduinoJson.h>

// Solo habilitamos los logs de debug en caso de ser necesario, para evitar carga extra en la versión en producción.
#define SERIAL_DEBUG_ENABLED 1

#if SERIAL_DEBUG_ENABLED
#define debug_print(message) \
  {                          \
    Serial.print(message);   \
  }
#define debug_println(message) \
  {                            \
    Serial.println(message);   \
  }
#else
#define debug_print(message)
#define debug_println(message)
#endif

// SENSORES
#define PIR_SENSOR 8
#define FLEX_SENSOR A0
#define ULTRASONIC_SENSOR_TRIGGER 7
#define ULTRASONIC_SENSOR_ECHO 6

// ACTUADORES
#define SERVOMOTOR 9

// CONSTANTES
#define MINIMUM_DIGITAL_VALUE 0
#define MAXIMUM_DIGITAL_VALUE 1023
#define MEDIAN_SAMPLE_SIZE 10         // TAMAÑO DE LA MEDIANA PARA LA CALIBRACION DE LOS SENSORES.
#define CALIBRATION_TIME 30           // TIEMPO DE CALIBRACION DE LOS SENSORES.
#define CALIBRATION_DELAY 1000        // DELAY PARA LA CALIBRACION DE LOS SENSORES.
#define GENERAL_TIMEOUT_LIMIT 50      // TEMPORIZADOR PARA LECTURA DE SENSORES.
#define PRESENCE_TIMEOUT_LIMIT 5000   // TEMPORIZADOR PARA DETECCION DE PRESENCIA.
#define MINIMUM_STATE_INDEX 0         // INDICE INFERIOR DE LA MATRIZ DE ESTADOS.
#define MAXIMUM_STATE_INDEX 3         // INDICE SUPERIOR DE LA MATRIZ DE ESTADOS.
#define MINIMUM_EVENT_INDEX 0         // INDICE INFERIOR DE LA MATRIZ DE EVENTOS.
#define MAXIMUM_EVENT_INDEX 9         // INDICE SUPERIOR DE LA MATRIZ DE EVENTOS.
#define MAXIMUM_INDEX_VERIFICATIONS 4 // INDICE SUPERIOR DE LA LISTA DE VERIFICACIONES.
#define DETECTION_TIMES 5             // CANTIDAD DE DETECCIONES CONSECUTIVAS NECESARIAS PARA UN CAMBIO DE ESTADO.
#define LID_CLOSED 0                  // ANGULO CORRESPONDIENTE A LA TAPA CERRADA.
#define LID_OPEN 180                  // ANGULO CORRESPONDIENTE A LA TAPA ABIERTA.

#define DEFAULT_MAXIMUM_FLEX_VALUE 500   // VALOR MAXIMO DEL SENSOR FLEX POR DEFECTO.
#define DEFAULT_MINIMUM_FLEX_VALUE 0     // VALOR MINIMO DEL SENSOR FLEX POR DEFECTO.
#define DEFAULT_MAXIMUM_WEIGHT_ALLOWED 3 // PESO MAXIMO (EN KG.) PERMITIDO POR DEFECTO.
#define DEFAULT_CONTAINER_SIZE 100       // TAMAÑO TOTAL (EN CM.) DEL CONTENEDOR POR DEFECTO.
#define DEFAULT_CRITICAL_PERCENTAGE 0.6  // PORCENTAJE DE DETECCION DE CAPACIDAD CRITICA POR DEFECTO.
#define DEFAULT_FULL_PERCENTAGE 0.8      // PORCENTAJE DE DETECCION DE CONTENEDOR LLENO POR DEFECTO.

#define LCD_MEMORY 32  // TAMAÑO TOTAL DEL DISPLAY LCD.
#define LCD_COLUMNS 16 // COLUMNAS DEL DISPLAY LCD.
#define LCD_ROWS 2     // FILAS DEL DISPLAY LCD.

// Obtenemos el valor 0.01715 de la siguiente manera:
// La velocidad del sonido es 0.0343 centimetros por microsegundo.
// Lo dividimos por 2 porque solo nos interesa la mitad del tiempo que tarda en ir y volver la onda de sonido.
// AL multiplicar este valor por el tiempo leido en el sensor, obtenemos la distancia en centímetros.
#define PULSE_DURATION_TO_DISTANCE_FACTOR 0.01715

// Definimos una zona muerta para que alcanzar el valor crítico de distancia no haga transiciones constantes por fluctuaciones en la lectura de los sensores.
#define DEADBAND 10

// Utilizamos un pulso disparador de 10 microsegundos para el correcto funcionamiento del sensor ultrasonido porque es lo recomendado.
#define TRIGGER_PULSE 10

// MENSAJES
#define MESSAGE_MAINTENANCE_NEEDED "EL TACHO NECESITA MANTENIMIENTO"
#define MESSAGE_REQUEST_DISABLING "TACHO FUERA DE SERVICIO"
#define MESSAGE_MAINTENANCE "EL TACHO ESTA EN MANTENIMIENTO"
#define MESSAGE_MAINTENANCE_FINISHED "MANTENIMIENTO TERMINADO"
#define MESSAGE_ERROR "LA OPERACION NO ES VALIDA PARA EL ESTADO ACTUAL"

// COMANDOS
#define CODE_CONNECTION_REQUESTED 0

#define CODE_UPDATE_REQUESTED 10
#define CODE_START_MAINTENANCE 11
#define CODE_MAINTENANCE_FINISHED 12
#define CODE_DISABLE 13

#define CODE_CONFIGURE_THRESHOLDS 90
#define CODE_CALIBRATE_PIR 91
#define CODE_CALIBRATE_MAXIMUM_CAPACITY 92
#define CODE_CALIBRATE_WEIGHT 93

#define CODE_UPDATE_STATUS "update"
#define CODE_ACK "ack"
#define CODE_ERROR "error"
#define CODE_CALIBRATION_STARTED "calst"
#define CODE_CALIBRATION_FINISHED "calend"

#define COMMAND_KEY_CODE "c"
#define COMMAND_KEY_DATA "d"
#define COMMAND_KEY_FULL_PERCENTAGE "fp"
#define COMMAND_KEY_CRITICAL_PERCENTAGE "cp"
#define COMMAND_KEY_MAXIMUM_WEIGHT "mw"
#define COMMAND_KEY_CONTAINER_SIZE "cs"
#define COMMAND_KEY_CURRENT_PERCENTAGE "p"

// TIPOS DE DATOS
typedef enum status
{
  ST_UNF,      // STATUS UNFILLED
  ST_CRIT_CAP, // STATUS CRITICAL CAPACITY
  ST_NO_CAP,   // STATUS NO CAPACITY
  ST_MAINT     // STATUS MAINTENANCE
} t_status;

typedef enum event
{
  EV_CONT,    // EVENT CONTINUE
  EV_NPD,     // EVENT NO PRESENCE DETECTED
  EV_PD,      // EVENT PRESENCE DETECTED
  EV_MAX_WR,  // EVENT MAX WEIGHT REACHED
  EV_UNF,     // EVENT UNFILLED
  EV_CCR,     // EVENT CRITICAL CAPACITY REACHED
  EV_MCR,     // EVENT MAXIMUM CAPACITY REACHED
  EV_SM,      // EVENT SEND TO MAINTENANCE / START MAINTENANCE?
  EV_MF,      // EVENT MAINTENANCE FINISHED
  EV_DIS,     // EVENT DISABLED
  EV_CONN_REQ // CONNECTION REQUEST
} t_event;

typedef void (*t_actions)();

typedef struct st_timer
{
  unsigned long previous_time;
  unsigned long current_time;
  bool timeout;
  unsigned long limit;
} t_timer;

typedef struct st_bluetooth_data
{
  int maximum_weight;
  double critical_percentage;
  double full_percentage;
} t_bluetooth_data;

typedef struct st_bluetooth_message
{
  int code;
  t_bluetooth_data data;
} t_bluetooth_message;

typedef struct st_bluetooth_response
{
  String code;
  int maximum_weight;
  double critical_percentage;
  double full_percentage;
  int container_size;
  String status;
} t_bluetooth_response;

typedef bool (*t_verification)();

// FUNCIONES DE TRANSICION
void none();
void open();
void close();
void disable();
void send_maintenance();
void request_disabling();
void reset();
void error();
void log_current_status();
void confirm_connection();

// FUNCIONES PARA VERIFICAR EVENTOS
bool verify_presence();
bool verify_weight();
bool verify_message();
bool verify_capacity();

// FUNCIONES GENERALES
bool timeout_reached(t_timer *timer);
void reset_timer(t_timer *timer);
void move_servomotor(byte degrees);
void initialize();
long get_echo(int pin);
double get_distance(int pin);
void show_status(t_status status);
void log_current_status();
void log(const char *message);
void calibrate_pir();
void calibrate_flex_sensor();
void calibrate_ultrasonic_sensor();
int calculate_median(double *arr, size_t size);
void insertion_sort(double *arr, size_t size);
void update_display(String message);
void display_print_optimal_split(const String &message);
int find_optimal_split_index(const String &message, int maxChars);
bool try_deserialize(String serializedData, t_bluetooth_message *output);
bool process_detection(int *detection_counter, t_event transition_event);
void notify(DynamicJsonDocument doc);
void calibration_started();
void calibration_finished();

// VARIABLES GLOBALES
const int colorR = 255;
const int colorG = 0;
const int colorB = 0;

int container_size = DEFAULT_CONTAINER_SIZE;
int flex_max_value = DEFAULT_MAXIMUM_FLEX_VALUE;
int flex_min_value = DEFAULT_MINIMUM_FLEX_VALUE;

double critical_percentage = DEFAULT_CRITICAL_PERCENTAGE;
double full_percentage = DEFAULT_FULL_PERCENTAGE;
int maximum_weight_allowed = DEFAULT_MAXIMUM_WEIGHT_ALLOWED;

int is_open = 0;
int presence_detection_counter = 0;
int critical_capacity_detection_counter = 0;
int full_capacity_detection_counter = 0;
int maximum_weight_detection_counter = 0;
int capacity_available_detection_counter = 0;
double current_percentage = 0.0;

PWMServo servo;
rgb_lcd display;
SoftwareSerial bluetooth_serial(10, 11); // RX | TX

t_timer timer_general;
t_timer timer_presence;
t_timer timer_connection_request;

t_status current_state;
t_status previous_state;
t_event current_event;
byte index_verification;

// MAQUINA DE ESTADO
t_actions action[MAXIMUM_STATE_INDEX + 1][MAXIMUM_EVENT_INDEX + 1] =
    {
        {none, close, open, disable, none, none, disable, error, none, error},              // ST_UNF
        {none, close, open, disable, none, none, disable, error, error, request_disabling}, // ST_CRIT_CAP
        {none, none, none, none, none, none, none, send_maintenance, error, none},          // ST_NO_CAP
        {none, none, none, none, none, none, none, error, reset, error},                    // ST_MAINT
                                                                                            // EV_CONT        EV_NPD      EV_PD      EV_MAX_WR      EV_UNF      EV_CCR      EV_MCR        EV_SM               EV_MF      EV_DIS                 EV_CON_REQ
};

t_status transition[MAXIMUM_STATE_INDEX + 1][MAXIMUM_EVENT_INDEX + 1] =
    {
        {
            ST_UNF,
            ST_UNF,
            ST_UNF,
            ST_NO_CAP,
            ST_UNF,
            ST_CRIT_CAP,
            ST_NO_CAP,
            ST_UNF,
            ST_UNF,
            ST_UNF,

        },                                                                                                                       // ST_UNF
        {ST_CRIT_CAP, ST_CRIT_CAP, ST_CRIT_CAP, ST_NO_CAP, ST_UNF, ST_CRIT_CAP, ST_NO_CAP, ST_CRIT_CAP, ST_CRIT_CAP, ST_NO_CAP}, // ST_CRIT_CAP
        {ST_NO_CAP, ST_NO_CAP, ST_NO_CAP, ST_NO_CAP, ST_NO_CAP, ST_NO_CAP, ST_NO_CAP, ST_MAINT, ST_NO_CAP, ST_NO_CAP},           // ST_NO_CAP
        {ST_MAINT, ST_MAINT, ST_MAINT, ST_MAINT, ST_MAINT, ST_MAINT, ST_MAINT, ST_MAINT, ST_UNF, ST_MAINT},                      // ST_MAINT
                                                                                                                                 // EV_CONT        EV_NPD           EV_PD            EV_MAX_WR      EV_UNF         EV_CCR           EV_MCR         EV_SM            EV_MF            EV_DIS
};

const char *STATUS_DESCRIPTION[] =
    {
        "CAPACIDAD DISPONIBLE",
        "CAPACIDAD CRITICA",
        "SIN CAPACIDAD",
        "EN MANTENIMIENTO"};

const char *EVENT_DESCRIPTION[] =
    {
        "CONTINUAR",
        "SIN PRESENCIA",
        "PRESENCIA DETECTADA",
        "PESO MAXIMO ALCANZADO",
        "CAPACIDAD DISPONIBLE",
        "CAPACIDAD CRITICA ALCANZADA",
        "CAPACIDAD MAXIMA ALCANZADA",
        "ENVIAR A MANTENIMIENTO",
        "MANTENIMIENTO TERMINADO",
        "DESHABILITAR TACHO",
        "SOLICITUD DE CONEXION"};

t_verification verification[MAXIMUM_INDEX_VERIFICATIONS] =
    {
        verify_presence,
        verify_weight,
        verify_message,
        verify_capacity};

// INICIALIZACION
void setup()
{
  Serial.begin(9600);
  bluetooth_serial.begin(9600);

  // set up the LCD's number of columns and rows:
  display.begin(16, 2);
  display.setRGB(colorR, colorG, colorB);
  display_print_optimal_split("INICIANDO");
  pinMode(PIR_SENSOR, INPUT);
  pinMode(FLEX_SENSOR, INPUT);
  pinMode(ULTRASONIC_SENSOR_TRIGGER, INPUT);
  servo.attach(SERVOMOTOR);

  timer_general.limit = GENERAL_TIMEOUT_LIMIT;
  timer_presence.limit = PRESENCE_TIMEOUT_LIMIT;
  timer_connection_request.limit = PRESENCE_TIMEOUT_LIMIT;
  initialize();
  current_state = ST_UNF;
  previous_state = ST_UNF;

  show_status(current_state);
  log_current_status();
}

void loop()
{
  get_event();
  if (current_event >= MINIMUM_EVENT_INDEX &&
      current_event <= MAXIMUM_EVENT_INDEX &&
      current_state >= MINIMUM_STATE_INDEX &&
      current_state <= MAXIMUM_STATE_INDEX)
  {
    previous_state = current_state;
    action[current_state][current_event]();
    current_state = transition[current_state][current_event];
    if (previous_state != current_state)
    {
      show_status(current_state);
      notify_state();
      log_current_status();
    }
  }
  else
  {
    debug_println("ERROR: evento desconocido");
  }
}

// Se obtienen los eventos
void get_event()
{
  if (timeout_reached(&timer_general))
  {
    reset_timer(&timer_general);
    int index = (index_verification % MAXIMUM_INDEX_VERIFICATIONS);
    index_verification++;
    verification[index]();
  }
  else
  {
    current_event = EV_CONT;
  }
}

// Verifica si se detecta movimiento con el sensor PIR.
// Si no se detecta, verifica si se superó el tiempo de tolerancia. Si aún no expiró, lo considera como que aún hay presencia detectada.
bool verify_presence()
{
  int value = digitalRead(PIR_SENSOR);
  if (value == HIGH)
  {
    bool presence_detected = process_detection(&presence_detection_counter, EV_PD);
    if (presence_detected)
    {
      reset_timer(&timer_presence);
    }
    return true;
  }
  else
  {
    if (timer_presence.current_time > 0 && timeout_reached(&timer_presence))
    {
      current_event = EV_NPD;
    }
    return false;
  }
}

// Verifica si el peso excede los valores permitidos.
// En este caso, mapeamos los valores leídos del sensor flex a los grados que puede flexionarse.
// 0 - Nada flexionado,  1 - Completamente flexionado.
bool verify_weight()
{
  int flex_value = analogRead(FLEX_SENSOR);
  int currentWeight = map(flex_value, flex_min_value, flex_max_value, 0, maximum_weight_allowed);

  if (currentWeight >= maximum_weight_allowed)
  {
    process_detection(&maximum_weight_detection_counter, EV_MAX_WR);
  }
  return false;
}

// Simulación de la detección de órdenes por medio de la aplicación.
// Se reemplazará por la implementación real.
bool verify_message()
{
  bluetooth_serial.flush();
  if (bluetooth_serial.available())
  {
    // se los lee y se los muestra en el monitor serie
    String read = bluetooth_serial.readString();
    debug_println(read);
    bluetooth_serial.flush();
    read.trim();

    t_bluetooth_message bluetooth_message;
    bool success = try_deserialize(read, &bluetooth_message);

    if (success)
    {
      process_command(&bluetooth_message);
    }
  }
}

// Verifica el volumen ocupado.
bool verify_capacity()
{
  double distance = get_distance(ULTRASONIC_SENSOR_TRIGGER);
  current_percentage = 1 - (distance / (double)container_size);
  if (current_percentage >= full_percentage)
  {
    if (process_detection(&full_capacity_detection_counter, EV_MCR))
    {
      critical_capacity_detection_counter = 0;
      capacity_available_detection_counter = 0;
    }
  }
  else if (current_percentage >= critical_percentage)
  {
    if (process_detection(&critical_capacity_detection_counter, EV_CCR))
    {
      full_capacity_detection_counter = 0;
      capacity_available_detection_counter = 0;
    }
  }
  else if (current_percentage < critical_percentage)
  {
    if (process_detection(&capacity_available_detection_counter, EV_UNF))
    {
      full_capacity_detection_counter = 0;
      critical_capacity_detection_counter = 0;
    }
  }
  return true;
}

bool process_detection(int *detection_counter, t_event transition_event)
{
  (*detection_counter)++;
  if ((*detection_counter) >= DETECTION_TIMES)
  {
    *detection_counter = 0;
    current_event = transition_event;
    return true;
  }
  return false;
}

void process_command(const t_bluetooth_message *bluetooth_message)
{
  switch (bluetooth_message->code)
  {
  case CODE_START_MAINTENANCE:
    current_event = EV_SM;
    break;

  case CODE_DISABLE:
    current_event = EV_DIS;
    break;

  case CODE_MAINTENANCE_FINISHED:
    current_event = EV_MF;
    break;

  case CODE_CONFIGURE_THRESHOLDS:
    debug_println(bluetooth_message->data.full_percentage);
      debug_println(bluetooth_message->data.critical_percentage);
        debug_println(bluetooth_message->data.maximum_weight);
    full_percentage = bluetooth_message->data.full_percentage;
    critical_percentage = bluetooth_message->data.critical_percentage;
    maximum_weight_allowed = bluetooth_message->data.maximum_weight;
    break;

  case CODE_CONNECTION_REQUESTED:
    confirm_connection();
    break;
  case CODE_CALIBRATE_PIR:
    calibrate_pir();
    break;
  case CODE_CALIBRATE_MAXIMUM_CAPACITY:
    calibrate_ultrasonic_sensor();
    break;
  case CODE_CALIBRATE_WEIGHT:
    calibrate_flex_sensor();
    break;
  default:
    debug_println("COMANDO NO RECONOCIDO");
    break;
  }
}

bool try_deserialize(String serializedData, t_bluetooth_message *output)
{
  DynamicJsonDocument doc(60);
  DeserializationError error = deserializeJson(doc, serializedData);
  if (error)
  {
    debug_print(F("deserializeJson() failed: "));
    debug_println(error.f_str());
    output = NULL;
    return false;
  }
  t_bluetooth_message parsed_message;
  parsed_message.code = doc[COMMAND_KEY_CODE];
  if (doc.containsKey(COMMAND_KEY_DATA))
  {
    t_bluetooth_data bluetooth_data;
    bluetooth_data.critical_percentage = doc[COMMAND_KEY_DATA][COMMAND_KEY_CRITICAL_PERCENTAGE];
    bluetooth_data.full_percentage = doc[COMMAND_KEY_DATA][COMMAND_KEY_FULL_PERCENTAGE];
    bluetooth_data.maximum_weight = doc[COMMAND_KEY_DATA][COMMAND_KEY_MAXIMUM_WEIGHT];
    parsed_message.data = bluetooth_data;
  }
  *output = parsed_message;
  return true;
}

// Verifica si el timer alcanzó el timeout.
bool timeout_reached(t_timer *timer)
{
  timer->current_time = millis();
  unsigned long difference = (timer->current_time) - (timer->previous_time);
  timer->timeout = (difference > timer->limit) ? true : false;
  return timer->timeout;
}

// Reinicia el timer
void reset_timer(t_timer *timer)
{
  timer->timeout = false;
  timer->previous_time = millis();
  timer->current_time = millis();
}

void none()
{
}

// Abre la tapa del contenedor
void open()
{
  if (!is_open)
  {
    move_servomotor(LID_OPEN);
    is_open = 1;
  }
}

// Cierra la tapa del contenedor
void close()
{
  if (is_open)
  {
    move_servomotor(LID_CLOSED);
    is_open = 0;
  }
}

// Deshabilita el contenedor y notifica que se necesita mantenimiento
void disable()
{
  close();
}

// Deshabilita el contenedor y notifica que se solicitó deshabilitarlo.
void request_disabling()
{
  disable();
}

// Notifica que se inició el mantenimiento del contenedor.
void send_maintenance()
{
}

// Reinicia el sistema al estado inicial.
void reset()
{
  initialize();
  close();
}

// Inicializa el sistema.
void initialize()
{
  reset_timer(&timer_general);
  reset_timer(&timer_presence);
  reset_timer(&timer_connection_request);
  index_verification = 0;
}

void move_servomotor(byte degrees)
{
  servo.write(degrees);
}

// Obtiene la distancia en centímetros utilizando el factor de conversión.
double get_distance(int pin)
{
  return PULSE_DURATION_TO_DISTANCE_FACTOR * get_echo(pin);
}

// Obtiene el ancho de pulso del sensor.
long get_echo(int pin)
{
  pinMode(pin, OUTPUT);
  digitalWrite(pin, LOW);
  delayMicroseconds(2);
  digitalWrite(pin, HIGH);
  delayMicroseconds(TRIGGER_PULSE);
  digitalWrite(pin, LOW);
  pinMode(ULTRASONIC_SENSOR_ECHO, INPUT);
  return pulseIn(ULTRASONIC_SENSOR_ECHO, HIGH);
}

// Imprime el estado en la pantalla.
void show_status(t_status status)
{
  String message = STATUS_DESCRIPTION[status];
  display_print_optimal_split(message);
}

// Notifica el estado actual.
void notify_state()
{
  DynamicJsonDocument doc(20);
  doc[COMMAND_KEY_CODE] = CODE_UPDATE_STATUS;
  doc[COMMAND_KEY_DATA] = STATUS_DESCRIPTION[current_state];
  doc[COMMAND_KEY_CURRENT_PERCENTAGE] = current_percentage;
  serializeJson(doc, bluetooth_serial);
  //doc.shrinkToFit();
  //notify(doc);
}

void confirm_connection()
{
  DynamicJsonDocument doc(100);
  doc[COMMAND_KEY_CODE] = CODE_ACK;
  doc[COMMAND_KEY_CRITICAL_PERCENTAGE] = critical_percentage;
  doc[COMMAND_KEY_FULL_PERCENTAGE] = full_percentage;
  doc[COMMAND_KEY_MAXIMUM_WEIGHT] = maximum_weight_allowed;
  doc[COMMAND_KEY_DATA] = STATUS_DESCRIPTION[current_state];
  doc[COMMAND_KEY_CURRENT_PERCENTAGE] = current_percentage;
  serializeJson(doc, bluetooth_serial);
  //size_t capacity = measureJson(doc);
  //doc.shrinkToFit();
  //notify(doc);
}

// Notifica la ocurrencia de un error.
void error()
{
  DynamicJsonDocument doc(20);
  doc[COMMAND_KEY_CODE] = CODE_ERROR;
  doc[COMMAND_KEY_DATA] = MESSAGE_ERROR;
  serializeJson(doc, bluetooth_serial);
  //doc.shrinkToFit();
  //notify(doc);
}

void calibration_started()
{
  DynamicJsonDocument doc(20);
  doc[COMMAND_KEY_CODE] = CODE_CALIBRATION_STARTED;
  serializeJson(doc, bluetooth_serial);
  //doc.shrinkToFit();
  //notify(doc);
}

void calibration_finished()
{
  DynamicJsonDocument doc(20);
  doc[COMMAND_KEY_CODE] = CODE_CALIBRATION_FINISHED;
  serializeJson(doc, bluetooth_serial);
  //doc.shrinkToFit();
  //notify(doc);
}

void notify(DynamicJsonDocument doc)
{
  String serialized;
  serializeJson(doc, serialized);
  bluetooth_serial.println(serialized);
}

void log_current_status()
{
  debug_print("DEBUG: EVENTO ");
  debug_println(EVENT_DESCRIPTION[current_event]);
  debug_print("DEBUG: ESTADO ");
  debug_println(STATUS_DESCRIPTION[current_state]);
}

void calibrate_pir()
{
  calibration_started();
  display_print_optimal_split("CALIBRANDO PIR");
  bool is_calibrated = false;
  int calibrationProgress = 0;
  do
  {
    int value = digitalRead(PIR_SENSOR);
    if (value == HIGH)
    {
      calibrationProgress = 0;
      debug_println("Reinicio de calibracion");
    }
    else
    {
      calibrationProgress++;
    }
    if (calibrationProgress == CALIBRATION_TIME)
    {
      is_calibrated = true;
    }
    delay(CALIBRATION_DELAY);
  } while (!is_calibrated);
  debug_println("PIR calibrado exitosamente");
  calibration_finished();
}

void calibrate_ultrasonic_sensor()
{
  calibration_started();
  display_print_optimal_split("CALIBRANDO CAPACIDAD");
  double sample[MEDIAN_SAMPLE_SIZE];
  for (int i = 0; i < MEDIAN_SAMPLE_SIZE; i++)
  {
    double distance = get_distance(ULTRASONIC_SENSOR_TRIGGER);
    sample[i] = distance;
    delay(CALIBRATION_DELAY);
  }
  container_size = calculate_median(sample, MEDIAN_SAMPLE_SIZE);
  debug_print("Tam. del contenedor: ");
  debug_println(container_size);
  calibration_finished();
}
void calibrate_flex_sensor()
{
  int sensorValue;
  flex_min_value = MAXIMUM_DIGITAL_VALUE;
  flex_max_value = MINIMUM_DIGITAL_VALUE;

  calibration_started();
  display_print_optimal_split("CALIBRANDO PESO MINIMO");
  for (int i = 0; i < MEDIAN_SAMPLE_SIZE; i++)
  {
    sensorValue = analogRead(FLEX_SENSOR);
    flex_min_value = min(flex_min_value, sensorValue);
    delay(CALIBRATION_DELAY);
  }

  display_print_optimal_split("CALIBRANDO PESO MAXIMO");
  for (int i = 0; i < MEDIAN_SAMPLE_SIZE; i++)
  {
    sensorValue = analogRead(FLEX_SENSOR);
    flex_max_value = max(flex_max_value, sensorValue);
    delay(1000);
  }

  debug_print("Peso calibrado: ");
  debug_print(flex_min_value);
  debug_print(" - ");
  debug_println(flex_max_value);
  calibration_finished();
}

// Algoritmo de ordenación por inserción.
void insertion_sort(double *arr, size_t size)
{
  for (size_t i = 1; i < size; ++i)
  {
    double key = arr[i];
    size_t j = i;
    while (j > 0 && arr[j - 1] > key)
    {
      arr[j] = arr[j - 1];
      --j;
    }
    arr[j] = key;
  }
}

// Se calcula la mediana del array ordenandolo y tomando el o los valores intermedios.
int calculate_median(double *arr, size_t size)
{
  // Se ordena el array.
  insertion_sort(arr, size);

  // Cálculo de mediana.
  if (size % 2 == 0)
  {
    // Si el array tiene un numero par de elementos, se calcula el promedio entre los del medio.
    return (arr[size / 2 - 1] + arr[size / 2]) / 2;
  }
  else
  {
    // Si el array tiene un numero impar de elementos, se devuelve el valor del medio.
    return arr[size / 2];
  }
}

// Se imprime un mensaje con el salto de línea en el mejor lugar posible.
void display_print_optimal_split(const String &msg)
{
  display.clear();
  String message = msg;
  if (message.length() <= LCD_COLUMNS)
  {
    display_print(message, 0);
  }
  else
  {
    int split_index = find_optimal_split_index(message, LCD_COLUMNS);

    String line1 = message.substring(0, split_index);
    display_print(line1, 0);

    String line2 = message.substring(split_index);
    display_print(line2, 1);
  }
}

void display_print(String &message, int row)
{
  display.setCursor(get_center_index(message), row); // Se centra el texto.
  display.print(message);
}

int get_center_index(String &message)
{
  if (message.length() > LCD_COLUMNS)
  {
    message = message.substring(0, LCD_COLUMNS);
  }
  return (LCD_COLUMNS - message.length()) / 2;
}

int find_optimal_split_index(const String &message, int max_chars)
{
  int closest_space_index = -1;

  for (int i = max_chars - 1; i >= 0; i--)
  {
    if (message.charAt(i) == ' ')
    {
      closest_space_index = i;
      break;
    }
  }
  return closest_space_index != -1 ? closest_space_index : max_chars;
}