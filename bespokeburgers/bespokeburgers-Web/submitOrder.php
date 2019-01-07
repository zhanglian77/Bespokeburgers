<?php

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    header('Location: orderPage.php', true, 303);
    die();
} elseif ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (!isset($_POST['order'])){
        echo -1;
        die;
    }
    
    //this handy function from https://stackoverflow.com/a/834355
    function startsWith($haystack, $needle)
    {
        $length = strlen($needle);
        return (substr($haystack, 0, $length) === $needle);
    }
    
    $order = ($_POST['order']);
    
    $host = "127.0.0.1";
    $port = 9090;
    $socket = socket_create(AF_INET, SOCK_STREAM, 0) or die("Could not create socket\n");
    $result = socket_connect($socket, $host, $port) or die("Could not connect to server\n");
    
    $message1 = "RGSTR,WEB\r\n";
    socket_write($socket, $message1, strlen($message1)) or die("Could not send data to server\n");
    
    $message2 = urldecode($order);
    socket_write($socket, $message2, strlen($message2)) or die("Could not send data to server\n");
    
//     get server response
    $result = socket_read ($socket, 1024) or die("Could not read server response\n");
    
    $message3 = "DERGSTR\r\n";
    socket_write($socket, $message3, strlen($message3));
    socket_close($socket);
    
    echo $result; //to be handled by javascript
}
?>