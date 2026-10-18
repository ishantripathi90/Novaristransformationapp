<?php
/** Novaris Android companion account API. Never exposes assessment answers or reports.
 * HTTPS-only production JSON API: POST register/login/logout, GET me/health.
 * Requires PHP 8.1+, OpenSSL/standard PHP password_hash support; no database install.
 */
declare(strict_types=1);
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store, private');
header('X-Content-Type-Options: nosniff');
header('X-Frame-Options: DENY');
header('Referrer-Policy: no-referrer');

function response(int $status, array $data): never {
    http_response_code($status);
    echo json_encode($data, JSON_UNESCAPED_SLASHES | JSON_INVALID_UTF8_SUBSTITUTE);
    exit;
}
function app_error(int $status, string $message): never { response($status,['ok'=>false,'error'=>$message]); }
function clean_field(string $value, int $length): string {
    $value=trim(preg_replace('/[\x00-\x1F\x7F]/u',' ', $value) ?? '');
    return function_exists('mb_substr') ? mb_substr($value,0,$length,'UTF-8') : substr($value,0,$length);
}
function token_from_header(): string {
    $value=(string)($_SERVER['HTTP_AUTHORIZATION'] ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? '');
    if(!preg_match('/^Bearer\s+([A-Za-z0-9_-]{40,90})$/', $value, $m)) app_error(401,'Please sign in again.');
    return $m[1];
}
function public_user(array $record): array {
    return ['name'=>(string)$record['name'],'email'=>(string)$record['email'],'company'=>(string)($record['company']??'')];
}
function storage_dir(): string {
    // First choice: outside public_html, not accessible via the web server.
    $parent=dirname(__DIR__).'/novaris-app-private';
    $dir=$parent;
    if(!is_dir($dir) && !@mkdir($dir,0700,true)) {
        // Shared hosts may restrict parent access: deny any HTTP access explicitly.
        $dir=__DIR__.'/novaris-app-private';
        if(!is_dir($dir) && !@mkdir($dir,0700,true)) app_error(503,'Account storage is not writable on this host.');
    }
    if(!is_dir($dir) || !is_writable($dir)) app_error(503,'Account storage is not writable on this host.');
    @chmod($dir,0700);
    if(str_starts_with($dir,__DIR__.DIRECTORY_SEPARATOR)) {
        $deny=$dir.'/.htaccess';
        if(!file_exists($deny)) file_put_contents($deny, "Deny from all\n<IfModule mod_authz_core.c>\nRequire all denied\n</IfModule>\n");
        if(!file_exists($dir.'/index.html')) file_put_contents($dir.'/index.html','');
    }
    return $dir;
}
function db_transaction(callable $operation): mixed {
    $path=storage_dir().'/accounts.json';
    $fp=@fopen($path,'c+');
    if($fp===false) app_error(503,'Account storage is unavailable.');
    try {
        if(!flock($fp,LOCK_EX)) app_error(503,'Account storage is busy.');
        $raw=stream_get_contents($fp);
        $db=($raw && strlen($raw)>0)?json_decode($raw,true):null;
        if(!is_array($db)) $db=['users'=>[],'sessions'=>[],'attempts'=>[]];
        $db['users'] ??= []; $db['sessions'] ??= []; $db['attempts'] ??= [];
        if(!is_array($db['users']) || !is_array($db['sessions']) || !is_array($db['attempts'])) app_error(503,'Account storage format is invalid.');
        $now=time();
        foreach($db['sessions'] as $id=>$s) if(!is_array($s) || (int)($s['expires']??0)<$now) unset($db['sessions'][$id]);
        foreach($db['attempts'] as $id=>$a) if(!is_array($a) || (int)($a['since']??0)<$now-3600) unset($db['attempts'][$id]);
        $result=$operation($db);
        $json=json_encode($db,JSON_INVALID_UTF8_SUBSTITUTE);
        if($json===false) app_error(503,'Account storage could not be saved.');
        rewind($fp);
        if(!ftruncate($fp,0) || fwrite($fp,$json)!==strlen($json) || !fflush($fp)) app_error(503,'Account storage could not be saved.');
        @chmod($path,0600);
        return $result;
    } finally {
        @flock($fp,LOCK_UN);
        fclose($fp);
    }
}
function issue_session(array &$db, string $email): string {
    $token=rtrim(strtr(base64_encode(random_bytes(36)),'+/','-_'),'=');
    $db['sessions'][hash('sha256',$token)]=['email'=>$email,'expires'=>time()+30*86400];
    return $token;
}
function client_key(): string {
    $ip=(string)($_SERVER['REMOTE_ADDR']??'unknown');
    return hash('sha256', $ip);
}
function check_limit(array &$db,string $type,int $max,int $seconds): bool {
    $key=$type.':'.client_key();
    $entry=$db['attempts'][$key]??null;
    $now=time();
    if(!is_array($entry)||$now-(int)($entry['since']??0)>$seconds) $entry=['since'=>$now,'count'=>0];
    $entry['count']++;
    $db['attempts'][$key]=$entry;
    return $entry['count'] <= $max;
}
function json_body(): array {
    $type=strtolower((string)($_SERVER['CONTENT_TYPE']??''));
    if(!str_starts_with($type,'application/json')) app_error(415,'Please send JSON.');
    $raw=file_get_contents('php://input',false,null,0,12000);
    if(!is_string($raw)||strlen($raw)>11000) app_error(413,'Request too large.');
    $parsed=json_decode($raw);
    if(!($parsed instanceof stdClass)) app_error(400,'Invalid request.');
    return (array)$parsed;
}

$action=(string)($_GET['action']??'');
$method=(string)($_SERVER['REQUEST_METHOD']??'GET');
$secure=(!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS']!=='off') || strtolower((string)($_SERVER['HTTP_X_FORWARDED_PROTO']??''))==='https';
$host=strtolower((string)($_SERVER['HTTP_HOST']??''));
$local=in_array(explode(':',$host)[0],['localhost','127.0.0.1'],true);
if(!$secure && !$local) app_error(403,'Use HTTPS for account access.');
if($action==='health' && $method==='GET') response(200,['ok'=>true,'service'=>'Novaris Android Accounts','version'=>'1.0']);
if(!in_array($action,['register','login','logout','me'],true)) app_error(404,'Unknown action.');
if($method!==($action==='me'?'GET':'POST')) app_error(405,'Unsupported method.');

if($action==='register') {
    $data=json_body();
    $email=strtolower(trim((string)($data['email']??'')));
    $name=clean_field((string)($data['name']??''),100);
    $company=clean_field((string)($data['company']??''),120);
    $password=(string)($data['password']??'');
    if(strlen($name)<2 || !filter_var($email,FILTER_VALIDATE_EMAIL) || strlen($email)>200) app_error(422,'Please enter a valid name and email.');
    if(strlen($password)<8 || strlen($password)>200) app_error(422,'Password must be 8–200 characters.');
    $passwordHash=password_hash($password,PASSWORD_DEFAULT);
    if(!is_string($passwordHash)) app_error(503,'Password protection unavailable.');
    $result=db_transaction(static function(array &$db) use ($email,$name,$company,$passwordHash):array {
        if(!check_limit($db,'register',6,3600)) return ['_status'=>429,'error'=>'Too many attempts. Please try again later.'];
        if(isset($db['users'][$email])) return ['_status'=>409,'error'=>'An account with this email already exists.'];
        $record=['name'=>$name,'email'=>$email,'company'=>$company,'hash'=>$passwordHash,'created_at'=>time()];
        $db['users'][$email]=$record;
        return ['ok'=>true,'user'=>public_user($record),'token'=>issue_session($db,$email)];
    });
    if(isset($result['_status'])) app_error($result['_status'],$result['error']);
    response(201,$result);
}
if($action==='login') {
    $data=json_body();
    $email=strtolower(trim((string)($data['email']??'')));
    $password=(string)($data['password']??'');
    if(!filter_var($email,FILTER_VALIDATE_EMAIL)||$password==='') app_error(422,'Enter your email and password.');
    $result=db_transaction(static function(array &$db) use ($email,$password):array {
        if(!check_limit($db,'login',20,900)) return ['_status'=>429,'error'=>'Too many attempts. Please try again later.'];
        $record=$db['users'][$email]??null;
        if(!is_array($record)||!password_verify($password,(string)($record['hash']??''))) return ['_status'=>401,'error'=>'Incorrect email or password.'];
        return ['ok'=>true,'user'=>public_user($record),'token'=>issue_session($db,$email)];
    });
    if(isset($result['_status'])) app_error($result['_status'],$result['error']);
    response(200,$result);
}
if($action==='me') {
    $token=token_from_header();
    $result=db_transaction(static function(array &$db) use ($token):array {
        $session=$db['sessions'][hash('sha256',$token)]??null;
        if(!is_array($session)||((int)($session['expires']??0))<time()) app_error(401,'Your session expired. Please sign in again.');
        $record=$db['users'][$session['email']]??null;
        if(!is_array($record)) app_error(401,'Please sign in again.');
        return ['ok'=>true,'user'=>public_user($record)];
    });
    response(200,$result);
}
if($action==='logout') {
    $token=token_from_header();
    json_body();
    db_transaction(static function(array &$db) use ($token):null { unset($db['sessions'][hash('sha256',$token)]);return null; });
    response(200,['ok'=>true]);
}
