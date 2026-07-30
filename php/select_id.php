<?php
	include 'connection.php';
	$pacote = $_POST['pacote'];
	$query = mysqli_query($con, "SELECT * FROM msgpacotes WHERE pacote = '$pacote'");
	$data = array();
	$qry_array = array();
	$i = 0;
	$total = mysqli_num_rows($query);
	while ($row = mysqli_fetch_array($query)) {
	  $data['pacote'] = $row['pacote'];
	  $data['mensagem'] = $row['mensagem'];
	  $data['link'] = $row['link'];
	  $data['registro'] = $row['registro'];
	  $qry_array[$i] = $data;
	  $i++;
	}

	if($query){
	  $response['success'] = 'true';
	  $response['message'] = 'Data Loaded Successfully';
	  $response['total'] = $total;
	  $response['data'] = $qry_array;
	}else{
	  $response['success'] = 'false';
	  $response['message'] = 'Data Loading Failed';
	}

	echo json_encode($response);

	mysqli_close($con);
?>
