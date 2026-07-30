<?php
   	include 'connection.php';

	if(isset($_POST["pacote"])){
   		$pacote = $_POST["pacote"];
	}

	$query = mysqli_query($con, "SELECT registro FROM msgpacotes WHERE pacote='$pacote'");

	if (!$query) {
    		printf("Error: %s\n", mysqli_error($con));
 	   	exit();
	}

	$data = array();
	$qry_array = array();
	$i = 0;
	$total = mysqli_num_rows($query);
	while ($row = mysqli_fetch_array($query)) {
  		$data['registro'] = $row['registro'];
  		$qry_array[$i] = $data;
  		$i++;
	}

	if(!$query){
  		$response['success'] = 'false';
  		$response['message'] = 'Falha no carregamento de dados';
	} else {
		if ($total>0) {
			//$response['success'] = 'true';
  			//$response['message'] = 'Dados Carregados com sucesso';
			//$response['total'] = $total;
			$response['data'] = $qry_array;
			//$response['message'] = $pacote;
		} else {
			//$response['success'] = 'true';
  			//$response['message'] = 'Nenhum Dado Encontrado';
			//$response['total'] = $total;
			$response['data'] = $qry_array;
		}
	}

	echo json_encode($response);

	mysqli_close($con);
?>
